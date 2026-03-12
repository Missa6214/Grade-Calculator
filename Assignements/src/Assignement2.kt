sealed class NetworkState {
    object Loading : NetworkState()
    data class Success(val data: String) : NetworkState()
    data class Error(val message: String) : NetworkState()
}

fun handleState(state: NetworkState) {
    when (state) {
        is NetworkState.Loading -> {
            println("Loading... Please wait.")
        }
        is NetworkState.Success -> {
            println("Success: ${state.data}")
        }
        is NetworkState.Error -> {
            println("Error occurred: ${state.message}")
        }
    }
}

fun main() {
    val states = listOf(
        NetworkState.Loading,
        NetworkState.Success("User data loaded"),
        NetworkState.Error("Network timeout")
    )

    // 4. Itération
    states.forEach { handleState(it) }
}