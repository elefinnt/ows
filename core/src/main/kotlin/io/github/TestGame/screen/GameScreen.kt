package io.github.TestGame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.ExtendViewport
import io.github.TestGame.entity.Enemy
import io.github.TestGame.entity.Player
import io.github.TestGame.entity.PowerUp
import io.github.TestGame.ui.VirtualJoystick
import ktx.app.KtxScreen
import ktx.log.logger

class GameScreen : KtxScreen {
    private val viewport = ExtendViewport(16f, 9f)
    private val spriteBatch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val joystick: VirtualJoystick
    private val player = Player()

    // Terrain properties
    private val tileSize = 1f
    private val groundTile = Texture("graphics/Top-Down Simple Summer_Ground 14.png")

    // Power-ups
    private val powerUps = mutableMapOf<String, PowerUp>()

    // Enemies
    private val enemies = mutableMapOf<String, Enemy>()

    private val worldSeed = 12345

    init {
        // Create joystick
        joystick = VirtualJoystick(viewport)
    }

    override fun show() {
        log.debug { "Game screen shown" }
    }

    override fun render(delta: Float) {
        handleInput(delta)

        // Spawn and update enemies
        spawnEnemies()
        updateEnemies(delta)

        // Update player (pass enemies for attack detection)
        player.update(delta, enemies.values.toList())

        // Spawn power-ups in visible area
        spawnPowerUps()

        // Check power-up collisions
        checkPowerUpCollisions()

        // Make camera follow player (centered on player)
        viewport.camera.position.set(
            player.x + player.size / 2f,
            player.y + player.size / 2f,
            0f
        )
        viewport.camera.update()

        // Update viewport and set projection matrix
        viewport.apply()
        spriteBatch.projectionMatrix = viewport.camera.combined
        shapeRenderer.projectionMatrix = viewport.camera.combined

        // Draw everything in one batch
        spriteBatch.begin()

        // Draw terrain tiles
        drawProceduralTerrain()

        // Draw animated player
        player.render(spriteBatch)

        spriteBatch.end()

        // Draw power-ups
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        powerUps.values.forEach { it.render(shapeRenderer) }
        shapeRenderer.end()

        // Draw enemies (sprites)
        spriteBatch.begin()
        enemies.values.forEach { it.render(spriteBatch) }
        spriteBatch.end()

        // Draw joystick on top
        joystick.render(delta)
    }

    private fun spawnPowerUps() {
        // Calculate visible area based on camera position
        val camX = viewport.camera.position.x
        val camY = viewport.camera.position.y
        val halfWidth = viewport.worldWidth / 2f
        val halfHeight = viewport.worldHeight / 2f

        // Calculate tile range for spawning (with padding)
        val startTileX = ((camX - halfWidth - 2f) / tileSize).toInt()
        val endTileX = ((camX + halfWidth + 2f) / tileSize).toInt()
        val startTileY = ((camY - halfHeight - 2f) / tileSize).toInt()
        val endTileY = ((camY + halfHeight + 2f) / tileSize).toInt()

        // Spawn power-ups procedurally based on tile coordinates
        for (x in startTileX..endTileX) {
            for (y in startTileY..endTileY) {
                val key = "$x,$y"
                if (!powerUps.containsKey(key) && shouldSpawnPowerUp(x, y)) {
                    powerUps[key] = PowerUp(x.toFloat(), y.toFloat())
                }
            }
        }
    }

    private fun shouldSpawnPowerUp(x: Int, y: Int): Boolean {
        // Use hash function to determine if a power-up should spawn here
        var hash = worldSeed + 999  // Different offset from terrain
        hash = hash * 31 + x
        hash = hash * 31 + y
        hash = hash xor (hash shr 16)
        hash = hash * 0x45d9f3b.toInt()
        hash = hash xor (hash shr 13)

        val value = (hash and 0x7fffffff) % 100
        return value < 5  // 5% chance of spawning a power-up
    }

    private fun checkPowerUpCollisions() {
        powerUps.values.forEach { powerUp ->
            if (powerUp.checkCollision(player.x, player.y, player.size)) {
                powerUp.isCollected = true
                player.applySpeedBoost()
                log.debug { "Power-up collected! Speed boost activated!" }
            }
        }
    }

    private fun spawnEnemies() {
        // Calculate visible area based on camera position
        val camX = viewport.camera.position.x
        val camY = viewport.camera.position.y
        val halfWidth = viewport.worldWidth / 2f
        val halfHeight = viewport.worldHeight / 2f

        // Calculate tile range for spawning (with padding)
        val startTileX = ((camX - halfWidth - 3f) / tileSize).toInt()
        val endTileX = ((camX + halfWidth + 3f) / tileSize).toInt()
        val startTileY = ((camY - halfHeight - 3f) / tileSize).toInt()
        val endTileY = ((camY + halfHeight + 3f) / tileSize).toInt()

        // Spawn enemies procedurally based on tile coordinates
        for (x in startTileX..endTileX) {
            for (y in startTileY..endTileY) {
                val key = "$x,$y"
                if (!enemies.containsKey(key) && shouldSpawnEnemy(x, y)) {
                    enemies[key] = Enemy(x.toFloat(), y.toFloat())
                }
            }
        }
    }

    private fun shouldSpawnEnemy(x: Int, y: Int): Boolean {
        // Use hash function to determine if an enemy should spawn here
        var hash = worldSeed + 777  // Different offset from power-ups
        hash = hash * 31 + x
        hash = hash * 31 + y
        hash = hash xor (hash shr 16)
        hash = hash * 0x45d9f3b.toInt()
        hash = hash xor (hash shr 13)

        val value = (hash and 0x7fffffff) % 1000
        return value < 15  // 1.5% chance of spawning an enemy
    }

    private fun updateEnemies(delta: Float) {
        val deadEnemies = mutableListOf<String>()
        val explosionRadius = 3f  // Radius that affects other enemies
        
        // Update all enemies
        enemies.forEach { (key, enemy) ->
            val playerDamaged = enemy.update(delta, player.x, player.y, player.size)
            
            // Apply damage and knockback to player if hit
            if (playerDamaged) {
                val knockback = enemy.getKnockbackForPlayer(player.x, player.y, player.size)
                player.takeDamage(enemy.getDamage(), knockback.first, knockback.second)
                log.debug { "Player hit! Health: ${player.currentHealth}" }
            }
            
            // Check if this enemy just started exploding
            if (enemy.isExploding()) {
                val (explosionX, explosionY) = enemy.getCenterPosition()
                
                // Damage other nearby enemies
                enemies.forEach { (otherKey, otherEnemy) ->
                    if (key != otherKey && !otherEnemy.isDead()) {
                        if (otherEnemy.isInExplosionRadius(explosionX, explosionY, explosionRadius)) {
                            // Kill the enemy caught in explosion
                            otherEnemy.takeDamage(999f, 0f, 0f)
                            log.debug { "Enemy caught in explosion chain reaction!" }
                        }
                    }
                }
            }
            
            // Mark dead enemies for removal
            if (enemy.isDead()) {
                deadEnemies.add(key)
            }
        }
        
        // Remove dead enemies
        deadEnemies.forEach { key ->
            log.debug { "Enemy defeated!" }
            enemies[key]?.dispose()
            enemies.remove(key)
        }
    }

    private fun drawProceduralTerrain() {
        // Calculate visible area based on camera position
        val camX = viewport.camera.position.x
        val camY = viewport.camera.position.y
        val halfWidth = viewport.worldWidth / 2f
        val halfHeight = viewport.worldHeight / 2f

        // Calculate tile range to draw (with some padding)
        val startTileX = ((camX - halfWidth - tileSize) / tileSize).toInt()
        val endTileX = ((camX + halfWidth + tileSize) / tileSize).toInt()
        val startTileY = ((camY - halfHeight - tileSize) / tileSize).toInt()
        val endTileY = ((camY + halfHeight + tileSize) / tileSize).toInt()

        // Draw ground tiles
        for (x in startTileX..endTileX) {
            for (y in startTileY..endTileY) {
                spriteBatch.draw(
                    groundTile,
                    x * tileSize,
                    y * tileSize,
                    tileSize,
                    tileSize
                )
            }
        }
    }


    private fun handleInput(delta: Float) {
        var moveX = 0f
        var moveY = 0f

        // Get joystick input
        val joyX = joystick.getKnobPercentX()
        val joyY = joystick.getKnobPercentY()

        if (joyX != 0f || joyY != 0f) {
            moveX = joyX
            moveY = joyY
        }

        // Set player velocity (infinite scrolling - no bounds!)
        player.setVelocity(moveX, moveY)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
        joystick.resize(width, height)
    }

    override fun dispose() {
        spriteBatch.dispose()
        shapeRenderer.dispose()
        groundTile.dispose()
        joystick.dispose()
        player.dispose()

        // Dispose all enemies
        enemies.values.forEach { it.dispose() }
    }

    companion object {
        private val log = logger<GameScreen>()
    }
}
