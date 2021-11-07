package your.game

import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.TextComponent
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.ecs.systems.TemporalSystem
import com.github.dwursteisen.minigdx.file.Font
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.game.Storyboard
import com.github.dwursteisen.minigdx.game.StoryboardAction
import com.github.dwursteisen.minigdx.game.StoryboardEvent
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.dwursteisen.minigdx.input.Key

class StartGameEvent : StoryboardEvent
class ShowScoreEvent(val score: Int) : StoryboardEvent

class StartGameSystem : System(EntityQuery.none()) {

    override fun update(delta: Seconds, entity: Entity) = Unit

    override fun update(delta: Seconds) {
        if (input.isKeyJustPressed(Key.SPACE)) {
            emit(StartGameEvent())
        }
    }
}

class BlinkingSystem : TemporalSystem(0.3f, EntityQuery.of(TextComponent::class)) {

    var visible = true

    override fun update(delta: Seconds, entity: Entity) {
        if(visible) {
            entity.position.setLocalTranslation(x = 0f)
        } else {
            entity.position.setLocalTranslation(x = 60f)
        }
    }

    override fun timeElapsed() {
        visible = !visible
    }
}

@OptIn(ExperimentalStdlibApi::class)
class Title(override val gameContext: GameContext, val text: String = "Press Space to start") : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("title.protobuf")
    private val font by gameContext.fileHandler.get<Font>("font3")

    override fun createStoryBoard(event: StoryboardEvent): StoryboardAction {
        if (event is StartGameEvent) {
            return Storyboard.replaceWith { MyGame(gameContext) }
        } else {
            return Storyboard.stayHere()
        }
    }

    override fun createEntities(entityFactory: EntityFactory) {
        scene.nodes.forEach { node ->
            if (node.name == "chrono") {
                entityFactory.createText(text, font, node)
            } else {
                entityFactory.createFromNode(node)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(
            StartGameSystem(),
            BlinkingSystem()
        )
    }
}
