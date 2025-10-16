package io.github.TestGame.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.sqrt

/**
 * A virtual joystick for touch controls.
 * Appears wherever the user touches and allows dragging to control movement.
 */
class VirtualJoystick(private val viewport: Viewport) : Disposable {
    private val shapeRenderer = ShapeRenderer()
    
    // Joystick visual properties (in pixels)
    private var baseX = 0f
    private var baseY = 0f
    private val baseRadius = 80f
    private val knobRadius = 40f
    
    // Current knob position (in pixels, relative to base)
    private var knobOffsetX = 0f
    private var knobOffsetY = 0f
    
    // Touch tracking
    private var touchPointer = -1
    private var isActive = false
    
    // Colors - brighter for better visibility
    private val baseColor = Color(0.2f, 0.2f, 0.2f, 0.6f)  // Semi-transparent
    private val knobColor = Color(0.8f, 0.8f, 0.8f, 0.8f)  // Semi-transparent
    
    fun render(delta: Float) {
        handleTouch()
        draw()
    }
    
    private fun handleTouch() {
        // Check if we have an active touch
        if (touchPointer >= 0) {
            if (!Gdx.input.isTouched(touchPointer)) {
                // Touch released, hide joystick
                touchPointer = -1
                isActive = false
                knobOffsetX = 0f
                knobOffsetY = 0f
            } else {
                // Update knob position based on touch
                val touchX = Gdx.input.getX(touchPointer).toFloat()
                val touchY = (Gdx.graphics.height - Gdx.input.getY(touchPointer)).toFloat()
                
                knobOffsetX = touchX - baseX
                knobOffsetY = touchY - baseY
                
                // Clamp to base radius
                val distance = sqrt(knobOffsetX * knobOffsetX + knobOffsetY * knobOffsetY)
                if (distance > baseRadius) {
                    knobOffsetX = (knobOffsetX / distance) * baseRadius
                    knobOffsetY = (knobOffsetY / distance) * baseRadius
                }
            }
        } else {
            // Check for new touch anywhere on screen
            if (Gdx.input.isTouched) {
                val touchX = Gdx.input.x.toFloat()
                val touchY = (Gdx.graphics.height - Gdx.input.y).toFloat()
                
                // Place joystick base at touch position
                baseX = touchX
                baseY = touchY
                touchPointer = 0
                isActive = true
                knobOffsetX = 0f
                knobOffsetY = 0f
            }
        }
    }
    
    private fun draw() {
        // Only draw when joystick is active
        if (!isActive) return
        
        shapeRenderer.projectionMatrix = viewport.camera.combined
        
        // Convert pixel coordinates to world coordinates for rendering
        val worldPos = viewport.unproject(Vector2(baseX, baseY))
        val worldBaseRadius = baseRadius / Gdx.graphics.height * viewport.worldHeight
        val worldKnobRadius = knobRadius / Gdx.graphics.height * viewport.worldHeight
        
        val worldKnobOffsetX = knobOffsetX / Gdx.graphics.height * viewport.worldHeight
        val worldKnobOffsetY = knobOffsetY / Gdx.graphics.height * viewport.worldHeight
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        // Draw base circle
        shapeRenderer.color = baseColor
        shapeRenderer.circle(worldPos.x, worldPos.y, worldBaseRadius, 32)
        
        // Draw knob circle
        shapeRenderer.color = knobColor
        shapeRenderer.circle(
            worldPos.x + worldKnobOffsetX,
            worldPos.y + worldKnobOffsetY,
            worldKnobRadius,
            32
        )
        
        shapeRenderer.end()
    }
    
    /**
     * Returns the horizontal position of the knob as a percentage (-1 to 1)
     */
    fun getKnobPercentX(): Float {
        return knobOffsetX / baseRadius
    }
    
    /**
     * Returns the vertical position of the knob as a percentage (-1 to 1)
     */
    fun getKnobPercentY(): Float {
        return knobOffsetY / baseRadius
    }
    
    fun resize(width: Int, height: Int) {
        // No need to reposition - joystick appears where user touches
    }
    
    override fun dispose() {
        shapeRenderer.dispose()
    }
}

