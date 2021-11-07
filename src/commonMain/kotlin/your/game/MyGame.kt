package your.game

import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.TextComponent
import com.github.dwursteisen.minigdx.ecs.components.text.WriteText
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.events.Event
import com.github.dwursteisen.minigdx.ecs.physics.AABBCollisionResolver
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.ecs.systems.TemporalSystem
import com.github.dwursteisen.minigdx.file.Font
import com.github.dwursteisen.minigdx.file.Sound
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.game.Storyboard
import com.github.dwursteisen.minigdx.game.StoryboardAction
import com.github.dwursteisen.minigdx.game.StoryboardEvent
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.dwursteisen.minigdx.input.InputHandler
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Interpolations
import com.github.dwursteisen.minigdx.math.Vector3
import your.game.EnemyMove.Companion.WAITING_HIT
import kotlin.random.Random

class Chrono(var time: Seconds = 60f) : Component
class Score(var score: Int = 0, var coolDown: Seconds = 0.4f /* = kotlin.Float */) : Component

class Player(
    var score: Int = 0,
    var life: Int = 3,
    var coolDown: Seconds = PlayerSystem.COOLDOWN
) : Component

class Enemy(
    var life: Int = 1,
    var key: Key? = null,
    var ttl: Seconds = 0f,
    var duration: Seconds = 0.3f,
    var start: Vector3 = Vector3(),
    var target: Vector3 = Vector3(),
    var ready: Boolean = false,
    var waitingPoint: Entity? = null,
    var letter: Entity? = null,
    var waitingHit: Float = WAITING_HIT
) : Component {

    fun switchTo(waitingPoint: Entity) {
        target.set(waitingPoint.position.localTranslation)
        this.waitingPoint = waitingPoint
        waitingPoint.get(Start::class).occupied = true
        ready = false
    }
}

class Dying(val letter: Entity) : Component

class Hit : Component
class Spawn : Component
class Start(val intex: Int = 1, var occupied: Boolean = false) : Component

class Left : Component
class Right : Component

class Track(val target: Entity) : Component

class FreeWaingPoint(
    val index: Int,
    // left or right
    val isLeft: Boolean,
    val newWaitingPoint: Entity
) : Event

class EnemyHit() : Event

class EndGame() : Event

class ScoreSystem : System(EntityQuery.of(Score::class, TextComponent::class)) {

    val players by interested(EntityQuery.of(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        val s = players.first().get(Player::class).score.toString()
        entity.get(TextComponent::class).text.content = "score: $s pts"
        val score = entity.get(Score::class)
        score.coolDown -= delta
        if (score.coolDown <= 0f) {
            score.coolDown = 0f
        }
    }

    override fun onEvent(event: Event, entityQuery: EntityQuery?) {
        if (event is EnemyHit) {
            entities.forEach {
                val coolDown = it.get(Score::class).coolDown
                if (coolDown <= 0f) {
                    it.position.setLocalScale(x = 1.5f, y = 1.5f)
                }
            }
        }
    }
}

class TrackSystem : System(EntityQuery.of(Track::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.position.setLocalTranslation(x = entity.get(Track::class).target.position.localTranslation.x)
    }
}

class EnemySpawner(val maxLife: Int = 3, val leftKeySet: Set<Key>, val rightKeySet: Set<Key>) : TemporalSystem(0.5f) {

    override fun update(delta: Seconds, entity: Entity) = Unit

    private val rightSpawn by interested(EntityQuery(Spawn::class, Right::class))
    private val leftSpawn by interested(EntityQuery(Spawn::class, Left::class))

    private val rightStart by interested(EntityQuery(Start::class, Right::class))
    private val leftStart by interested(EntityQuery(Start::class, Left::class))

    private val rightByIndex = mutableListOf<Entity>()
    private val leftByIndex = mutableListOf<Entity>()

    override fun onGameStarted(engine: Engine) {
        rightByIndex.addAll(rightStart.sortedBy { it.get(Start::class).intex })
        leftByIndex.addAll(leftStart.sortedBy { it.get(Start::class).intex })
    }

    private fun firstAvailable(points: List<Entity>): Entity? {
        val target = points.lastOrNull { it.get(Start::class).occupied }
        // no one is occupied
        return if (target == null) {
            points.first()
        } else if (target.get(Start::class).intex == 5) {
            null
        } else {
            points.first { it.get(Start::class).intex == target.get(Start::class).intex + 1 }
        }
    }

    override fun timeElapsed() {

        val isLeft = Random.nextFloat() > 0.5f

        if (isLeft) {
            val target = firstAvailable(leftByIndex)
            if (target == null) {
                return
            }
            val spawn = leftSpawn.first()
            create(Left(), spawn, target, leftKeySet)
        } else {
            val target = firstAvailable(rightByIndex)
            if (target == null) {
                return
            }
            val spawn = rightSpawn.first()
            create(Right(), spawn, target, rightKeySet)
        }
    }

    val keyToStr = mapOf(
        Key.A to "A",
        Key.B to "B",
        Key.C to "C",
        Key.D to "D",
        Key.E to "E",
        Key.F to "F",
        Key.G to "G",
        Key.H to "H",
        Key.I to "I",
        Key.J to "J",
        Key.K to "K",
        Key.L to "L",
        Key.M to "M",
        Key.N to "N",
        Key.O to "O",
        Key.P to "P",
        Key.Q to "Q",
        Key.R to "R",
        Key.S to "S",
        Key.T to "T",
        Key.U to "U",
        Key.V to "V",
        Key.W to "W",
        Key.X to "X",
        Key.Y to "Y",
        Key.Z to "Z"
    )

    private fun create(side: Component, spawn: Entity, waitingPoint: Entity, keySet: Set<Key>): Entity {
        val entity = entityFactory.createFromTemplate("enemy1")

        entity.add(side)

        entity.get(Enemy::class).key = keySet.random()
        val numberOfLive = numberOfLive()
        entity.get(Enemy::class).life = numberOfLive
        entity.get(Enemy::class).letter!!.get(TextComponent::class).text.content =
            keyToStr[entity.get(Enemy::class).key]!!

        entity.position.setLocalScale(y = getScale(numberOfLive))
        entity.position.setLocalTranslation(spawn.position.localTranslation)
        entity.get(Enemy::class).start.set(spawn.position.localTranslation)

        entity.get(Enemy::class).target.set(waitingPoint.position.localTranslation)
        entity.get(Enemy::class).waitingPoint = waitingPoint
        waitingPoint.get(Start::class).occupied = true
        return entity
    }

    companion object {

        fun getScale(life: Int): Float {
            return when (life) {
                0 -> 0.5f
                1 -> 0.75f
                2 -> 1f
                else -> 1f
            }
        }
    }

    private fun numberOfLive(): Int {
        if (maxLife == 1) {
            return 1
        } else if (maxLife == 2) {
            if (Random.nextFloat() > 0.75f) {
                return 2
            } else {
                return 1
            }
        } else {
            if (Random.nextFloat() < 0.5f) {
                return 1
            } else if (Random.nextFloat() < 0.80f) {
                return 2
            } else {
                return 3
            }
        }
    }
}

class EnemyMove : System(EntityQuery.Companion.of(Enemy::class)) {

    private val collider = AABBCollisionResolver()

    override fun update(delta: Seconds, entity: Entity) {
        val enemy = entity.get(Enemy::class)
        if (enemy.ready) {
            if (enemy.waitingPoint!!.get(Start::class).intex == 1) {
                enemy.waitingHit -= delta
                if (enemy.waitingHit < 0f) {
                    enemy.waitingHit = WAITING_HIT

                }
            }
            return
        }

        val x = Interpolations.linear.interpolate(
            enemy.start.x,
            enemy.target.x,
            enemy.ttl / enemy.duration
        )
        entity.position.setLocalTranslation(x = x)

        enemy.ttl += delta

        if (collider.collide(enemy.waitingPoint!!, entity)) {
            enemy.ready = true
            entity.position.setLocalTranslation(enemy.waitingPoint!!.position.localTranslation)
        }
    }

    override fun onEvent(event: Event, entityQuery: EntityQuery?) {
        if (event is FreeWaingPoint) {
            entities.forEach { enemy ->
                if (enemy.get(Enemy::class).waitingPoint!!.get(Start::class).intex == event.index) {
                    if (event.isLeft && enemy.hasComponent(Left::class)) {
                        freeWaitingPoint(enemy)
                        enemy.get(Enemy::class).switchTo(event.newWaitingPoint)
                    } else if (!event.isLeft && enemy.hasComponent(Right::class)) {
                        freeWaitingPoint(enemy)
                        enemy.get(Enemy::class).switchTo(event.newWaitingPoint)
                    }
                }
            }
        }
    }

    companion object {

        const val WAITING_HIT = 1f
    }
}

class EnemyDying : System(EntityQuery.of(Dying::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.get(Dying::class).letter.destroy()
        emit(EnemyHit())
        entity.destroy()
    }
}

fun InputHandler.isAnyJustKeyPressed(vararg keys: Key): Boolean {
    return keys.any { isKeyJustPressed(it) }
}

class PlayerSystem(leftKeySet: Set<Key>, rightKeySet: Set<Key>, val sound: List<Sound>) :
    System(EntityQuery.Companion.of(Player::class)) {

    private val left = leftKeySet.toTypedArray()
    private val right = rightKeySet.toTypedArray()

    val leftEnemies by interested(EntityQuery.Companion.of(Enemy::class, Left::class))
    val rightEnemies by interested(EntityQuery.Companion.of(Enemy::class, Right::class))

    val hits by interested(EntityQuery.of(Hit::class))

    val collider = AABBCollisionResolver()

    override fun update(delta: Seconds, entity: Entity) {
        if (entity.get(Player::class).coolDown <= 0f) {
            if (input.isAnyJustKeyPressed(*left)) {
                entity.position.setLocalTranslation(x = -1f)
                entity.get(Player::class).coolDown = COOLDOWN

                leftEnemies.forEach { enemy ->
                    if (enemy.get(Enemy::class).waitingPoint!!.get(Start::class).intex == 1 && enemy.get(Enemy::class).ready) {
                        if (input.isKeyJustPressed(enemy.get(Enemy::class).key!!)) {
                            hitEnemy(enemy)
                        }
                    }
                }
            } else if (input.isAnyJustKeyPressed(*right)) {
                entity.position.setLocalTranslation(x = 1f)
                entity.get(Player::class).coolDown = COOLDOWN

                rightEnemies.forEach { enemy ->
                    if (enemy.get(Enemy::class).waitingPoint!!.get(Start::class).intex == 1 && enemy.get(Enemy::class).ready) {
                        if (input.isKeyJustPressed(enemy.get(Enemy::class).key!!)) {
                            hitEnemy(enemy)
                        }
                    }
                }
            }
        } else {
            entity.get(Player::class).coolDown -= delta
        }

        entity.position.setLocalTranslation(
            x = Interpolations.lerp(0f, entity.position.localTranslation.x)
        )
    }

    private fun hitEnemy(enemy: Entity) {
        val component = enemy.get(Enemy::class)
        component.life--
        if (component.life < 0) {
            sound.random().play()
            enemy.remove(Enemy::class)
            enemy.add(Dying(component.letter!!))
            freeWaitingPoint(enemy)
        } else {
            enemy.position.setLocalScale(y = EnemySpawner.getScale(component.life))
        }
    }

    override fun onEvent(event: Event, entityQuery: EntityQuery?) {
        if (event is EnemyHit) {
            entities.forEach {
                it.get(Player::class).life -= 1
                if (it.get(Player::class).life < 0) {
                    emit(EndGame())
                }

                it.get(Player::class).score += 1
            }
        }
    }

    companion object {

        const val COOLDOWN = 0.125f
    }
}

fun System.freeWaitingPoint(enemy: Entity) {
    val component = enemy.get(Enemy::class)
    val waitingPoint = component.waitingPoint!!.get(Start::class)
    waitingPoint.occupied = false
    emit(FreeWaingPoint(waitingPoint.intex + 1, enemy.hasComponent(Left::class), component.waitingPoint!!))
}

@OptIn(ExperimentalStdlibApi::class)
class MyGame(override val gameContext: GameContext, val isAzerty: Boolean = true) : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("map.protobuf")

    private val font by gameContext.fileHandler.get<Font>("font3")
    private val a by gameContext.fileHandler.get<Sound>("a.mp3")
    private val b by gameContext.fileHandler.get<Sound>("b.mp3")
    private val c by gameContext.fileHandler.get<Sound>("c.mp3")

    override fun createStoryBoard(event: StoryboardEvent): StoryboardAction {
        return if (event is ShowScoreEvent) {
            Storyboard.replaceWith {
                Title(
                    gameContext,
                    "Share your (incredible) score of ${event.score}!\nPress Space to restart"
                )
            }
        } else {
            Storyboard.stayHere()
        }
    }

    override fun createEntities(entityFactory: EntityFactory) {
        // Create all entities needed at startup
        // The scene is the node graph that can be updated in Blender
        scene.nodes.forEach { node ->
            // Create an entity using all information from this node (model, position, camera, ...)

            if (node.name.contains("player")) {
                val entity = entityFactory.createFromNode(node)
                entity.add(Player())
            } else if (node.name.startsWith("enemy")) {
                entityFactory.registerTemplate(node.name) {

                    val e = entityFactory.createFromNode(node)
                        .add(Enemy())

                    val letter = entityFactory.createText(WriteText(" "), font)
                    letter.add(Track(e))

                    e.get(Enemy::class).letter = letter
                    // letter.attachTo(e)
                    letter.position.setLocalTranslation(y = 4f)
                    e
                }
            } else if (node.name == "hit") {
                entityFactory.createFromNode(node).add(Hit())
            } else if (node.name.startsWith("spawn-left")) {
                entityFactory.createFromNode(node).add(Spawn()).add(Left())
            } else if (node.name.startsWith("spawn-right")) {
                entityFactory.createFromNode(node).add(Spawn()).add(Right())
            } else if (node.name.startsWith("start-left")) {
                entityFactory.createFromNode(node).add(Start(extractIndex(node.name))).add(Left())
            } else if (node.name.startsWith("start-right")) {
                entityFactory.createFromNode(node).add(Start(extractIndex(node.name))).add(Right())
            } else if (node.name.startsWith("chrono")) {
                entityFactory.createText(WriteText("60:00"), font, node).add(Chrono())
            } else if (node.name.startsWith("score")) {
                entityFactory.createText(WriteText("0000"), font, node).add(Score())
            } else {
                entityFactory.createFromNode(node)
            }
        }
    }

    private fun extractIndex(name: String): Int {
        return name.last().toString().toInt()
    }

    override fun createSystems(engine: Engine): List<System> {
        val (leftKeySet, rightKeySet) = if (isAzerty) {
            setOf(
                Key.S, Key.D, Key.F
            ) to setOf(
                Key.J, Key.K, Key.L
            )
        } else {
            // Workman layout see https://workmanlayout.org/
            setOf(
                Key.S, Key.H, Key.T
            ) to setOf(
                Key.N, Key.E, Key.O
            )
        }

        // Create all systems used by the game
        return listOf(
            PlayerSystem(leftKeySet, rightKeySet, listOf(a, b, c)),
            EnemySpawner(leftKeySet = leftKeySet, rightKeySet = rightKeySet),
            EnemyMove(),
            EnemyDying(),
            TrackSystem(),
            ScoreSystem(),
            ChronoSystem()
        )
    }
}

class ChronoSystem : TemporalSystem(0.01f, EntityQuery.Companion.of(Chrono::class, TextComponent::class)) {

    val score by interested(EntityQuery.of(Player::class))

    override fun update(delta: Seconds, entity: Entity) {
        entity.get(Chrono::class).time -= delta

        if (entity.get(Chrono::class).time < 0f) {
            val s = score.first().get(Player::class).score

            emit(ShowScoreEvent(s))
        }
    }

    override fun timeElapsed() {
        entities.forEach {
            val t = it.get(Chrono::class).time

            val seconds = t.toInt()
            val millis = ((t - seconds) * 100).toInt()
            it.get(TextComponent::class).text.content = "$seconds:$millis"
        }
    }
}
