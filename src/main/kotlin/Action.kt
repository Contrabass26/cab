// An action that the player can do. Disregards details that are beyond the player's control e.g. deck randomness.
enum class Action(val branch: (FullGameState) -> List<FullGameState>) {

    ME_PLAY_CARD({ state ->
        with(state) {
            assert(stage == GameStage.ME_DOWN)
            myCards.map { copy(stage = GameStage.OPP_UP, myCards = myCards - it, centreCards = centreCards + it) }
        }
    }),

    OPP_PLAY_CARD({ state ->
        with(state) {
            assert(stage == GameStage.OPP_DOWN)
            oppCards.map { copy(stage = GameStage.ME_UP, oppCards = oppCards - it, centreCards = centreCards + it) }
        }
    }),

    ME_KNOCK({ state ->
        with(state) {
            assert(stage == GameStage.ME_UP)
            listOf(copy(stage = GameStage.OPP_UP, meKnocked = true))
        }
    }),

    OPP_KNOCK({ state ->
        with(state) {
            assert(stage == GameStage.ME_UP)
            listOf(copy(stage = GameStage.ME_UP, meKnocked = false))
        }
    }),

    ME_PICK_BLIND({ state ->
        with(state) {
            assert(stage == GameStage.ME_UP)
            getPickupDeck().map { copy(stage = GameStage.ME_DOWN, myCards = myCards + it) }
        }
    }),

    OPP_PICK_BLIND({ state ->
        with(state) {
            assert(stage == GameStage.OPP_UP)
            getPickupDeck().map { copy(stage = GameStage.OPP_DOWN, oppCards = oppCards + it) }
        }
    }),

    ME_PICK_VISIBLE({ state ->
        with(state) {
            val visibleCard = centreCards.firstOrNull()
            assert(stage == GameStage.ME_UP && visibleCard != null)
            listOf(
                copy(
                    stage = GameStage.ME_DOWN,
                    myCards = myCards + visibleCard!!,
                    centreCards = centreCards - visibleCard
                )
            )
        }
    }),

    OPP_PICK_VISIBLE({ state ->
        with(state) {
            val visibleCard = centreCards.firstOrNull()
            assert(stage == GameStage.OPP_UP && visibleCard != null)
            listOf(
                copy(
                    stage = GameStage.OPP_DOWN,
                    oppCards = oppCards + visibleCard!!,
                    centreCards = centreCards - visibleCard
                )
            )
        }
    });
}