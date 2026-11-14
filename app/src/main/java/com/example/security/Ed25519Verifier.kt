package com.example.security

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * High-performance, zero-dependency Ed25519 digital signature verifier (RFC 8032).
 *
 * Execution Strategy:
 * 1. Attempts platform Java Security `Signature.getInstance("Ed25519")` (native on Android 13+ / API 33+).
 * 2. Falls back to pure Kotlin BigInteger RFC 8032 implementation for full backwards compatibility
 *    with earlier Android versions (API 24 to 32).
 */
object Ed25519Verifier {

    // RFC 8032 constants for Curve25519
    private val P = BigInteger.ONE.shiftLeft(255).subtract(BigInteger.valueOf(19))
    private val L = BigInteger.ONE.shiftLeft(252).add(BigInteger("27742317777372353535851937790883648493"))
    private val D = P.subtract(BigInteger.valueOf(121665)).multiply(BigInteger.valueOf(121666).modInverse(P)).mod(P)
    private val I = BigInteger.valueOf(2).modPow(P.subtract(BigInteger.ONE).divide(BigInteger.valueOf(4)), P)

    private val Bx = BigInteger("216936d3cd6e53fec0a4e231fdd6dc5c692cc7609525a7b2c9562d608f25d51a", 16)
    private val By = BigInteger.valueOf(4).multiply(BigInteger.valueOf(5).modInverse(P)).mod(P)
    private val B = Point(Bx, By)

    data class Point(val x: BigInteger, val y: BigInteger)

    private fun pointAdd(p1: Point, p2: Point): Point {
        val x1x2 = p1.x.multiply(p2.x).mod(P)
        val y1y2 = p1.y.multiply(p2.y).mod(P)
        val dxxyy = D.multiply(x1x2).mod(P).multiply(y1y2).mod(P)

        val numX = p1.x.multiply(p2.y).add(p1.y.multiply(p2.x)).mod(P)
        val denX = BigInteger.ONE.add(dxxyy).mod(P)
        val x3 = numX.multiply(denX.modInverse(P)).mod(P)

        val numY = y1y2.add(x1x2).mod(P)
        val denY = BigInteger.ONE.subtract(dxxyy).add(P).mod(P)
        val y3 = numY.multiply(denY.modInverse(P)).mod(P)

        return Point(x3, y3)
    }

    private fun scalarMult(p: Point, sVal: BigInteger): Point {
        var q = Point(BigInteger.ZERO, BigInteger.ONE)
        var curr = p
        var s = sVal
        while (s > BigInteger.ZERO) {
            if (s.testBit(0)) {
                q = pointAdd(q, curr)
            }
            curr = pointAdd(curr, curr)
            s = s.shiftRight(1)
        }
        return q
    }

    private fun recoverX(y: BigInteger, signBit: Int): BigInteger? {
        if (y >= P) return null
        val u = y.multiply(y).subtract(BigInteger.ONE).add(P).mod(P)
        val v = D.multiply(y).multiply(y).add(BigInteger.ONE).mod(P)
        val x = u.multiply(v.modInverse(P)).mod(P)
        var xRoot = x.modPow(P.add(BigInteger.valueOf(3)).divide(BigInteger.valueOf(8)), P)
        if (xRoot.multiply(xRoot).subtract(x).mod(P) != BigInteger.ZERO) {
            xRoot = xRoot.multiply(I).mod(P)
        }
        if (xRoot.multiply(xRoot).subtract(x).mod(P) != BigInteger.ZERO) {
            return null
        }
        if (xRoot.testBit(0) != (signBit == 1)) {
            xRoot = P.subtract(xRoot).mod(P)
        }
        return xRoot
    }

    private fun decodePoint(bytes: ByteArray): Point? {
        if (bytes.size != 32) return null
        var y = BigInteger.ZERO
        for (i in 0 until 32) {
            val b = (bytes[i].toInt() and 0xFF).toLong()
            y = y.or(BigInteger.valueOf(b).shiftLeft(8 * i))
        }
        val signBit = if (y.testBit(255)) 1 else 0
        y = y.clearBit(255)
        val x = recoverX(y, signBit) ?: return null
        return Point(x, y)
    }

    private fun decodeScalar(bytes: ByteArray): BigInteger {
        var s = BigInteger.ZERO
        for (i in bytes.indices) {
            val b = (bytes[i].toInt() and 0xFF).toLong()
            s = s.or(BigInteger.valueOf(b).shiftLeft(8 * i))
        }
        return s
    }

    /**
     * Verifies an Ed25519 digital signature against a 32-byte public key and arbitrary message bytes.
     */
    fun verify(publicKeyBytes: ByteArray, messageBytes: ByteArray, signatureBytes: ByteArray): Boolean {
        if (publicKeyBytes.size != 32 || signatureBytes.size != 64) return false

        // 1. Try Java Security standard Ed25519 (supported on Android 13+ / Java 15+)
        try {
            val spkiHeader = byteArrayOf(
                0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
            )
            val fullSpki = spkiHeader + publicKeyBytes
            val keyFactory = KeyFactory.getInstance("Ed25519")
            val pubKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(fullSpki))
            val sig = Signature.getInstance("Ed25519")
            sig.initVerify(pubKey)
            sig.update(messageBytes)
            if (sig.verify(signatureBytes)) {
                return true
            }
        } catch (_: Throwable) {
            // Fall back to pure RFC 8032 implementation
        }

        // 2. Pure RFC 8032 verification
        return try {
            val a = decodePoint(publicKeyBytes) ?: return false
            val rBytes = signatureBytes.copyOfRange(0, 32)
            val sBytes = signatureBytes.copyOfRange(32, 64)
            val r = decodePoint(rBytes) ?: return false
            val s = decodeScalar(sBytes)
            if (s >= L) return false

            val md = MessageDigest.getInstance("SHA-512")
            md.update(rBytes)
            md.update(publicKeyBytes)
            md.update(messageBytes)
            val h = md.digest()
            val k = decodeScalar(h).mod(L)

            val sB = scalarMult(B, s)
            val kA = scalarMult(a, k)
            val rPlusKa = pointAdd(r, kA)

            sB.x == rPlusKa.x && sB.y == rPlusKa.y
        } catch (_: Exception) {
            false
        }
    }
}
