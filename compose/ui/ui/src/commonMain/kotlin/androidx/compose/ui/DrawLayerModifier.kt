/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.util.annotation.FloatRange
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Default camera distance for all layers
 */
const val DefaultCameraDistance = 8.0f

/**
 * Constructs a [TransformOrigin] from the given fractional values from the Layer's
 * width and height
 */
@Suppress("NOTHING_TO_INLINE")
inline fun TransformOrigin(pivotFractionX: Float, pivotFractionY: Float): TransformOrigin =
    TransformOrigin(packFloats(pivotFractionX, pivotFractionY))

/**
 * A two-dimensional position represented as a fraction of the Layer's width and height
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Immutable
inline class TransformOrigin(@PublishedApi internal val packedValue: Long) {

    /**
     * Return the position along the x-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the width of the content. A value of 0.5f represents the midpoint between the left
     * and right bounds of the content
     */
    val pivotFractionX: Float
        get() = unpackFloat1(packedValue)

    /**
     * Return the position along the y-axis that should be used as the
     * origin for rotation and scale transformations. This is represented as a fraction
     * of the height of the content. A value of 0.5f represents the midpoint between the top
     * and bottom bounds of the content
     */
    val pivotFractionY: Float
        get() = unpackFloat2(packedValue)

    /**
     * Returns a copy of this TransformOrigin instance optionally overriding the
     * pivotFractionX or pivotFractionY parameter
     */
    fun copy(
        pivotFractionX: Float = this.pivotFractionX,
        pivotFractionY: Float = this.pivotFractionY
    ) = TransformOrigin(pivotFractionX, pivotFractionY)

    companion object {

        /**
         * [TransformOrigin] constant to indicate that the center of the content should
         * be used for rotation and scale transformations
         */
        val Center = TransformOrigin(0.5f, 0.5f)
    }
}

/**
 * A [Modifier.Element] that makes content draw into a draw layer. The draw
 * layer can be invalidated separately from parents. A [drawLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * A [DrawLayerModifier] can also be used to apply effects to content, such as
 * scaling ([scaleX], [scaleY]), rotation ([rotationX], [rotationY], [rotationZ]),
 * opacity ([alpha]), shadow ([shadowElevation], [shape]), and clipping ([clip], [shape]).
 * Changes to most properties will not invalidate the contents. If set up correctly,
 * animating these properties can avoid composition, layout, and drawing.
 *
 * @sample androidx.compose.ui.samples.AnimateFadeIn
 * @see drawLayer
 */
interface DrawLayerModifier : Modifier.Element {
    /**
     * The horizontal scale of the drawn area. This would typically default to `1`.
     */
    val scaleX: Float get() = 1f

    /**
     * The vertical scale of the drawn area. This would typically default to `1`.
     */
    val scaleY: Float get() = 1f

    /**
     * The alpha of the drawn area. Setting this to something other than `1`
     * will cause the drawn contents to be translucent and setting it to `0` will
     * cause it to be fully invisible.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val alpha: Float
        get() = 1f

    /**
     * Horizontal pixel offset of the layer relative to its left bound
     */
    val translationX: Float get() = 0f

    /**
     * Vertical pixel offset of the layer relative to its top bound
     */
    val translationY: Float get() = 0f

    /**
     * Sets the elevation for the shadow in pixels. With the [shadowElevation] > 0f and
     * [shape] set, a shadow is produced.
     */
    @get:FloatRange(from = 0.0, to = 3.4e38 /* POSITIVE_INFINITY */)
    val shadowElevation: Float
        get() = 0f

    /**
     * The rotation of the contents around the horizontal axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationX: Float
        get() = 0f

    /**
     * The rotation of the contents around the vertical axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationY: Float
        get() = 0f

    /**
     * The rotation of the contents around the Z axis in degrees.
     */
    @get:FloatRange(from = 0.0, to = 360.0)
    val rotationZ: Float
        get() = 0f

    /**
     * Sets the distance along the Z axis (orthogonal to the X/Y plane on which
     * layers are drawn) from the camera to this layer. The camera's distance
     * affects 3D transformations, for instance rotations around the X and Y
     * axis. If the rotationX or rotationY properties are changed and this view is
     * large (more than half the size of the screen), it is recommended to always
     * use a camera distance that's greater than the height (X axis rotation) or
     * the width (Y axis rotation) of this view.
     *
     * The distance of the camera from the drawing plane can have an affect on the
     * perspective distortion of the layer when it is rotated around the x or y axis.
     * For example, a large distance will result in a large viewing angle, and there
     * will not be much perspective distortion of the view as it rotates. A short
     * distance may cause much more perspective distortion upon rotation, and can
     * also result in some drawing artifacts if the rotated view ends up partially
     * behind the camera (which is why the recommendation is to use a distance at
     * least as far as the size of the view, if the view is to be rotated.)
     *
     * The distance is expressed in pixels and must always be positive
     */
    @get:FloatRange(from = 0.0, to = 3.4e38 /* POSITIVE_INFINITY */)
    val cameraDistance: Float
        get() = DefaultCameraDistance

    /**
     * Offset percentage along the x and y axis for which contents are rotated and scaled.
     * The default value of 0.5f, 0.5f indicates the pivot point will be at the midpoint of the
     * left and right as well as the top and bottom bounds of the layer
     */
    val transformOrigin: TransformOrigin get() = TransformOrigin.Center

    /**
     * The [Shape] of the layer. When [shadowElevation] is non-zero a shadow is produced using
     * this [shape]. When [clip] is `true` contents will be clipped to this [shape].
     * When clipping, the content will be redrawn when the [shape] changes.
     */
    val shape: Shape get() = RectangleShape

    /**
     * Set to `true` to clip the content to the [shape].
     */
    val clip: Boolean get() = false
}

private class SimpleDrawLayerModifier(
    override val scaleX: Float,
    override val scaleY: Float,
    override val alpha: Float,
    override val translationX: Float,
    override val translationY: Float,
    override val shadowElevation: Float,
    override val rotationX: Float,
    override val rotationY: Float,
    override val rotationZ: Float,
    override val cameraDistance: Float,
    override val transformOrigin: TransformOrigin,
    override val shape: Shape,
    override val clip: Boolean,
    inspectorInfo: InspectorInfo.() -> Unit
) : DrawLayerModifier, InspectorValueInfo(inspectorInfo) {

    override fun hashCode(): Int {
        var result = scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + translationX.hashCode()
        result = 31 * result + translationY.hashCode()
        result = 31 * result + shadowElevation.hashCode()
        result = 31 * result + rotationX.hashCode()
        result = 31 * result + rotationY.hashCode()
        result = 31 * result + rotationZ.hashCode()
        result = 31 * result + cameraDistance.hashCode()
        result = 31 * result + transformOrigin.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + clip.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        val otherModifier = other as? SimpleDrawLayerModifier ?: return false
        return scaleX == otherModifier.scaleX &&
            scaleY == otherModifier.scaleY &&
            alpha == otherModifier.alpha &&
            translationX == otherModifier.translationX &&
            translationY == otherModifier.translationY &&
            shadowElevation == otherModifier.shadowElevation &&
            rotationX == otherModifier.rotationX &&
            rotationY == otherModifier.rotationY &&
            rotationZ == otherModifier.rotationZ &&
            cameraDistance == otherModifier.cameraDistance &&
            transformOrigin == otherModifier.transformOrigin &&
            shape == otherModifier.shape &&
            clip == otherModifier.clip
    }

    override fun toString(): String =
        "SimpleDrawLayerModifier(" +
            "scaleX=$scaleX, " +
            "scaleY=$scaleY, " +
            "alpha = $alpha, " +
            "translationX=$translationX, " +
            "translationY=$translationY, " +
            "shadowElevation=$shadowElevation, " +
            "rotationX=$rotationX, " +
            "rotationY=$rotationY, " +
            "rotationZ=$rotationZ, " +
            "cameraDistance=$cameraDistance, " +
            "transformOrigin=$transformOrigin, " +
            "shape=$shape, " +
            "clip=$clip)"
}

/**
 * Creates a [DrawLayerModifier] to have all content will be drawn into a new draw layer. The draw
 * layer can be invalidated separately from parents. A [drawLayer] should be used when the content
 * updates independently from anything above it to minimize the invalidated content.
 *
 * [drawLayer] can also be used to apply effects to content, such as scaling ([scaleX], [scaleY]),
 * rotation ([rotationX], [rotationY], [rotationZ]), opacity ([alpha]), shadow
 * ([shadowElevation], [shape]), and clipping ([clip], [shape]).
 *
 * @sample androidx.compose.ui.samples.ChangeOpacity
 *
 * @param scaleX [DrawLayerModifier.scaleX]
 * @param scaleY [DrawLayerModifier.scaleY]
 * @param alpha [DrawLayerModifier.alpha]
 * @param shadowElevation [DrawLayerModifier.shadowElevation]
 * @param rotationX [DrawLayerModifier.rotationX]
 * @param rotationY [DrawLayerModifier.rotationY]
 * @param rotationZ [DrawLayerModifier.rotationZ]
 * @param shape [DrawLayerModifier.shape]
 * @param clip [DrawLayerModifier.clip]
 */
@Stable
fun Modifier.drawLayer(
    scaleX: Float = 1f,
    scaleY: Float = 1f,
    alpha: Float = 1f,
    translationX: Float = 0f,
    translationY: Float = 0f,
    shadowElevation: Float = 0f,
    rotationX: Float = 0f,
    rotationY: Float = 0f,
    rotationZ: Float = 0f,
    cameraDistance: Float = DefaultCameraDistance,
    transformOrigin: TransformOrigin = TransformOrigin.Center,
    shape: Shape = RectangleShape,
    clip: Boolean = false
) = this.then(
    SimpleDrawLayerModifier(
        scaleX = scaleX,
        scaleY = scaleY,
        alpha = alpha,
        translationX = translationX,
        translationY = translationY,
        shadowElevation = shadowElevation,
        rotationX = rotationX,
        rotationY = rotationY,
        rotationZ = rotationZ,
        cameraDistance = cameraDistance,
        transformOrigin = transformOrigin,
        shape = shape,
        clip = clip,
        inspectorInfo = debugInspectorInfo {
            name = "drawLayer"
            properties["scaleX"] = scaleX
            properties["scaleY"] = scaleY
            properties["alpha"] = alpha
            properties["translationX"] = translationX
            properties["translationY"] = translationY
            properties["shadowElevation"] = shadowElevation
            properties["rotationX"] = rotationX
            properties["rotationY"] = rotationY
            properties["rotationZ"] = rotationZ
            properties["cameraDistance"] = cameraDistance
            properties["transformOrigin"] = transformOrigin
            properties["shape"] = shape
            properties["clip"] = clip
        }
    )
)