package com.example.core.util

import kotlin.math.abs

object QrCodeGenerator {

  /**
   * Generates a deterministic 25x25 matrix representing a QR-like verification code.
   * Includes standard finder patterns at top-left, top-right, and bottom-left,
   * timing patterns, and data hash encoding.
   */
  fun generateMatrix(content: String, size: Int = 25): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    // Helper to draw standard 7x7 finder pattern
    fun drawFinder(startX: Int, startY: Int) {
      for (r in 0 until 7) {
        for (c in 0 until 7) {
          val isOuter = r == 0 || r == 6 || c == 0 || c == 6
          val isInner = r in 2..4 && c in 2..4
          matrix[startY + r][startX + c] = isOuter || isInner
        }
      }
    }

    // Draw 3 Corner Finder Patterns
    drawFinder(0, 0)
    drawFinder(size - 7, 0)
    drawFinder(0, size - 7)

    // Timing patterns
    for (i in 8 until size - 8) {
      matrix[6][i] = (i % 2 == 0)
      matrix[i][6] = (i % 2 == 0)
    }

    // Deterministic payload hashing into data regions
    val hash = abs(content.hashCode())
    var bitIndex = 0
    for (r in 0 until size) {
      for (c in 0 until size) {
        // Skip finder pattern zones
        val inTopLeft = r < 8 && c < 8
        val inTopRight = r < 8 && c >= size - 8
        val inBottomLeft = r >= size - 8 && c < 8
        val inTiming = r == 6 || c == 6

        if (!inTopLeft && !inTopRight && !inBottomLeft && !inTiming) {
          val charVal = if (content.isNotEmpty()) content[bitIndex % content.length].code else 0
          val bit = ((hash xor (r * 31 + c * 17) xor charVal) ushr (bitIndex % 16)) and 1
          matrix[r][c] = (bit == 1)
          bitIndex++
        }
      }
    }

    return matrix
  }
}
