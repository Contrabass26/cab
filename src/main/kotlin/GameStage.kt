enum class GameStage(val isOpp: Boolean, val myHandSize: Int, val oppHandSize: Int, val actions: (FullGameState) -> Iterable<Action>) {
    ME_UP(false , 3, 3, {
        val actions = mutableSetOf(Action.ME_PICK_BLIND)
        if (it.myCards.getHandValue() >= 26)
            actions.add(Action.ME_KNOCK)
        if (it.centreCards.isNotEmpty())
            actions.add(Action.ME_PICK_VISIBLE)
        actions
    }),
    ME_DOWN(false, 4, 3, { state ->
        state.myCards.map { Action.ME_PLAY_CARD(it) }
    }),
    OPP_UP(true, 3, 3, {
        val actions = mutableSetOf(Action.OPP_PICK_BLIND)
        if (it.oppCards.getHandValue() >= 26)
            actions.add(Action.OPP_KNOCK)
        if (it.centreCards.isNotEmpty())
            actions.add(Action.OPP_PICK_VISIBLE)
        actions
    }),
    OPP_DOWN(true, 3, 4, { state ->
        state.oppCards.map { Action.OPP_PLAY_CARD(it) }
    });
}