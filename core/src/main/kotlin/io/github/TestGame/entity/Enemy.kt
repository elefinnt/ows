package io.github.TestGame.entity

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import kotlin.math.sqrt

/**
 * An animated enemy bat that chases the player when in range.
 */
class Enemy(var x: Float, var y: Float) : Disposable {
    private val texture: Texture = Texture("enemy/Mutated_Bat.png")
    private val frameWidth = 96
    private val frameHeight = 80
    val size = 3f  // Enemy size in world units
    private val moveSpeed = 2.5f
    private val detectionRange = 7f  // Medium distance to start chasing
    private val attackRange = 1.5f  // Range to start attack wind-up

    // Health system
    private val maxHealth = 100f
    var currentHealth = maxHealth
        private set
    private var isHit = false
    private var hitFlashTime = 0f
    private val hitFlashDuration = 0.1f

    // Knockback
    private var knockbackVelocityX = 0f
    private var knockbackVelocityY = 0f
    private val knockbackDecay = 8f

    // Attack system
    private var attackWindUpTime = 0f
    private val attackWindUpDuration = 0.75f  // Time before explosion
    private var isAttacking = false
    private var attackAnimationTime = 0f
    private val attackAnimationDuration = 0.64f  // 8 frames * 0.08s
    private val damageFrame = 4  // Frame at which damage occurs
    private val damageTime = damageFrame * 0.08f  // Time when damage occurs (0.32s)
    private var hasDamagedThisAttack = false
    private val attackDamage = 15f
    private val attackKnockback = 8f

    // Animation state
    private var stateTime = 0f
    private val idleAnimation: Animation<TextureRegion>
    private val attackAnimation: Animation<TextureRegion>

    init {
        val columns = texture.width / frameWidth
        val rows = texture.height / frameHeight

        val frames = Array(rows) {row ->
            Array(columns) { col ->
                TextureRegion(texture, col * frameWidth, row * frameHeight, frameWidth, frameHeight)
            }
        }

        idleAnimation = createAnimationFromRow(frames, 0, 0.15f, 6)
        attackAnimation = createAnimationFromRow(frames, 1, 0.08f, 8)
    }

    private fun createAnimationFromRow(frames: Array<Array<TextureRegion>>, row: Int, frameDuration: Float, frameCount: Int = -1): Animation<TextureRegion> {
        val animationFrames = com.badlogic.gdx.utils.Array<TextureRegion>()
        if (row < frames.size) {
            val maxFrames = if (frameCount > 0) frameCount else frames[row].size
            for (i in 0 until maxFrames.coerceAtMost(frames[row].size)) {
                animationFrames.add(frames[row][i])
            }
        }
        return Animation(frameDuration, animationFrames, Animation.PlayMode.LOOP)
    }
    /**
     * Updates the enemy AI and animation
     * Returns true if player was damaged this frame
     */
    fun update(delta: Float, playerX: Float, playerY: Float, playerSize: Float): Boolean {
        stateTime += delta
        var playerDamaged = false

        // Update hit flash timer
        if (hitFlashTime > 0f) {
            hitFlashTime -= delta
            if (hitFlashTime <= 0f) {
                isHit = false
            }
        }

        // Apply knockback
        if (knockbackVelocityX != 0f || knockbackVelocityY != 0f) {
            x += knockbackVelocityX * delta
            y += knockbackVelocityY * delta

            // Decay knockback
            knockbackVelocityX -= knockbackVelocityX * knockbackDecay * delta
            knockbackVelocityY -= knockbackVelocityY * knockbackDecay * delta

            // Stop when very small
            if (kotlin.math.abs(knockbackVelocityX) < 0.1f) knockbackVelocityX = 0f
            if (kotlin.math.abs(knockbackVelocityY) < 0.1f) knockbackVelocityY = 0f
        }

        // Calculate direction to player centre
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f

        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distance = sqrt(dx * dx + dy * dy)

        // Update attack state
        if (isAttacking) {
            attackAnimationTime += delta
            
            // Deal damage at frame 4
            if (!hasDamagedThisAttack && attackAnimationTime >= damageTime) {
                playerDamaged = true
                hasDamagedThisAttack = true
            }
            
            if (attackAnimationTime >= attackAnimationDuration) {
                // Attack finished - bat dies after explosion
                currentHealth = 0f
            }
        } else if (distance <= attackRange && knockbackVelocityX == 0f && knockbackVelocityY == 0f) {
            // In attack range, wind up
            attackWindUpTime += delta
            if (attackWindUpTime >= attackWindUpDuration) {
                // Start attack (no damage yet)
                isAttacking = true
                attackAnimationTime = 0f
                stateTime = 0f
                hasDamagedThisAttack = false
            }
        } else {
            // Reset wind-up if out of range
            attackWindUpTime = 0f
            
            // Move toward player if in detection range (and not being knocked back or attacking)
            if (distance <= detectionRange && distance > 0.1f && knockbackVelocityX == 0f && knockbackVelocityY == 0f) {
                val normalizedX = dx / distance
                val normalizedY = dy / distance

                x += normalizedX * moveSpeed * delta
                y += normalizedY * moveSpeed * delta
            }
        }

        return playerDamaged
    }

    /**
     * Gets knockback direction for player based on enemy position
     */
    fun getKnockbackForPlayer(playerX: Float, playerY: Float, playerSize: Float): Pair<Float, Float> {
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f

        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distance = sqrt(dx * dx + dy * dy)

        return if (distance > 0) {
            val knockbackX = (dx / distance) * attackKnockback
            val knockbackY = (dy / distance) * attackKnockback
            Pair(knockbackX, knockbackY)
        } else {
            Pair(0f, 0f)
        }
    }

    fun getDamage(): Float = attackDamage

    /**
     * Checks if this enemy is within explosion radius of a position
     */
    fun isInExplosionRadius(explosionX: Float, explosionY: Float, radius: Float): Boolean {
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f
        
        val dx = explosionX - enemyCenterX
        val dy = explosionY - enemyCenterY
        val distance = sqrt(dx * dx + dy * dy)
        
        return distance <= radius
    }

    /**
     * Gets the center position of this enemy for explosion calculations
     */
    fun getCenterPosition(): Pair<Float, Float> {
        return Pair(x + size / 2f, y + size / 2f)
    }

    /**
     * Returns true if this enemy is at the damage frame of explosion
     */
    fun isExploding(): Boolean = isAttacking && attackAnimationTime >= damageTime && attackAnimationTime < (damageTime + 0.1f)

    /**
     * Deals damage to the enemy and applies knockback
     * Returns false if damage was blocked (e.g. during explosion)
     */
    fun takeDamage(damage: Float, knockbackX: Float, knockbackY: Float): Boolean {
        // Immune to damage while exploding
        if (isAttacking) {
            return false
        }

        currentHealth -= damage
        if (currentHealth < 0f) currentHealth = 0f

        // Visual feedback
        isHit = true
        hitFlashTime = hitFlashDuration

        // Apply knockback
        knockbackVelocityX = knockbackX
        knockbackVelocityY = knockbackY
        
        return true
    }

    fun isDead(): Boolean = currentHealth <= 0f

    fun render(batch: SpriteBatch) {
        // Get current animation based on state
        val currentAnimation = if (isAttacking) attackAnimation else idleAnimation
        val currentFrame = currentAnimation.getKeyFrame(stateTime, !isAttacking)

        // Flash red when hit
        if (isHit) {
            batch.setColor(1f, 0.5f, 0.5f, 1f)
        }

        // Draw the sprite
        batch.draw(
            currentFrame,
            x,
            y,
            size,
            size
        )

        // Reset colour
        batch.setColor(1f, 1f, 1f, 1f)
    }

    /**
     * Renders the health bar above the enemy
     */
    fun renderHealthBar(shapeRenderer: ShapeRenderer) {
        val barWidth = size
        val barHeight = 0.2f
        val barX = x
        val barY = y + size + 0.1f

        // Background (red)
        shapeRenderer.color = Color.RED
        shapeRenderer.rect(barX, barY, barWidth, barHeight)

        // Foreground (green based on health percentage)
        val healthPercentage = currentHealth / maxHealth
        shapeRenderer.color = Color.GREEN
        shapeRenderer.rect(barX, barY, barWidth * healthPercentage, barHeight)
    }

    /**
     * Checks if the enemy is touching the player
     */
    fun checkCollision(playerX: Float, playerY: Float, playerSize: Float): Boolean {
        val playerCenterX = playerX + playerSize / 2f
        val playerCenterY = playerY + playerSize / 2f
        val enemyCenterX = x + size / 2f
        val enemyCenterY = y + size / 2f

        val dx = playerCenterX - enemyCenterX
        val dy = playerCenterY - enemyCenterY
        val distanceSquared = dx * dx + dy * dy
        val collisionRadius = (size / 2f) + (playerSize / 2f)

        return distanceSquared <= collisionRadius * collisionRadius
    }

    override fun dispose() {
        texture.dispose()
    }
}

