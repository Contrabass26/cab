enum class GameStage(val isOpp: Boolean, val myHandSize: Int, val oppHandSize: Int, vararg actions: Action) {
    ME_UP(false , 3, 3, Action.ME_KNOCK, Action.ME_PICK_BLIND, Action.ME_PICK_VISIBLE),
    ME_DOWN(false, 4, 3, Action.ME_PLAY_CARD),
    OPP_UP(true, 3, 3, Action.OPP_KNOCK, Action.OPP_PICK_BLIND, Action.OPP_PICK_VISIBLE),
    OPP_DOWN(true, 3, 4, Action.OPP_PLAY_CARD);

    val actions = actions.toSet()
}