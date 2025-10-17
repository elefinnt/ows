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
    private var enemy: Enemy? = null

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

        // Spawn and update enemy
        spawnEnemy()
        updateEnemy(delta)

        // Update player (pass enemies for attack detection)
        val enemies = enemy?.let { listOf(it) } ?: emptyList()
        player.update(delta, enemies)

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

        // Draw power-ups (circles)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        powerUps.values.forEach { it.render(shapeRenderer) }
        shapeRenderer.end()

        // Draw enemy (sprite)
        spriteBatch.begin()
        enemy?.render(spriteBatch)
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

    private fun spawnEnemy() {
        // Spawn one enemy if none exists
        if (enemy == null) {
            // Spawn enemy right on the player to see it immediately
            val spawnX = player.x
            val spawnY = player.y
            enemy = Enemy(spawnX, spawnY)
        }
    }

    private fun updateEnemy(delta: Float) {
        // Update the single enemy
        enemy?.update(delta, player.x, player.y, player.size)
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

        // Dispose enemy
        enemy?.dispose()
    }

    companion object {
        private val log = logger<GameScreen>()
    }
}
