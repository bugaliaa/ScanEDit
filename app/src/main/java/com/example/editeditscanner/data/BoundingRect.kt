package com.example.editeditscanner.data

import org.opencv.core.Point
import kotlin.math.abs

class BoundingRect {

    var topLeft: Point
    var topRight: Point
    var bottomLeft: Point
    var bottomRight: Point

    constructor() {
        topLeft = Point(0.0, 0.0)
        topRight = Point(0.0, 0.0)
        bottomLeft = Point(0.0, 0.0)
        bottomRight = Point(0.0, 0.0)
    }

    constructor(i: Double) {
        topLeft = Point(i, i)
        topRight = Point(i, i)
        bottomLeft = Point(i, i)
        bottomRight = Point(i, i)
    }

    fun fromPoints(points: MutableList<Point>, hRatio: Double, vRatio: Double) {
        topRight = Point(points[0].x * hRatio, points[0].y * vRatio)
        topLeft = Point(points[1].x * hRatio, points[1].y * vRatio)
        bottomLeft = Point(points[2].x * hRatio, points[2].y * vRatio)
        bottomRight = Point(points[3].x * hRatio, points[3].y * vRatio)
    }

    fun getTop(): Point {
        return Point((topLeft.x + topRight.x) / 2f, (topLeft.y + topRight.y) / 2f)
    }

    fun setTop(point: Point) {
        val top = getTop()
        val halfX = topLeft.x - top.x
        val halfY = topLeft.y - top.y
        topLeft = Point(halfX + point.x, halfY + point.y)
        topRight = Point(point.x - halfX, point.y - halfY)
    }

    fun getBottom(): Point {
        return Point((bottomLeft.x + bottomRight.x) / 2f, (bottomLeft.y + bottomRight.y) / 2f)
    }

    fun setBottom(point: Point) {
        val bottom = getBottom()
        val halfX = bottomLeft.x - bottom.x
        val halfY = bottomLeft.y - bottom.y
        bottomLeft = Point(halfX + point.x, halfY + point.y)
        bottomRight = Point(point.x - halfX, point.y - halfY)
    }

    fun getLeft(): Point {
        return Point((topLeft.x + bottomLeft.x) / 2f, (topLeft.y + bottomLeft.y) / 2f)
    }

    fun setLeft(point: Point) {
        val left = getLeft()
        val halfX = topLeft.x - left.x
        val halfY = topLeft.y - left.y
        topLeft = Point(halfX + point.x, halfY + point.y)
        bottomLeft = Point(point.x - halfX, point.y - halfY)
    }

    fun getRight(): Point {
        return Point((topRight.x + bottomRight.x) / 2f, (topRight.y + bottomRight.y) / 2f)
    }

    fun setRight(point: Point) {
        val right = getRight()
        val halfX = topRight.x - right.x
        val halfY = topRight.y - right.y
        topRight = Point(halfX + point.x, halfY + point.y)
        bottomRight = Point(point.x - halfX, point.y - halfY)
    }

    fun width(): Double {
        return abs(
            java.lang.Double.max(topRight.x, bottomRight.x) - java.lang.Double.min(
                topLeft.x,
                bottomLeft.x
            )
        )
    }

    fun height(): Double {
        return abs(
            java.lang.Double.max(
                bottomLeft.y,
                bottomRight.y
            ) - java.lang.Double.min(topLeft.y, topRight.x)
        )
    }

    override fun toString(): String {
        return "(topLeft=$topLeft, topRight=$topRight, bottomLeft=$bottomLeft, bottomRight=$bottomRight)"
    }
}