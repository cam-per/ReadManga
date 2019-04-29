package ru.garretech.readmanga

/**
 * Created by garred on 14.03.17.
 */

object Settings {
    const val client_id = "6863889"
    const val api_scope = "video,offline"
    const val api_display = "mobile"
    const val api_redirect_uri = "ttp://api.vk.com/blank.html"
    const val api_secret_key = "HMIqzRPUL4Shf9eOnjan"
    const val api_response_type = "token"
    const val version = "5.92"
    const val max_loaded_in_screen = 15
    const val BLOCK_ID = "adf-304149/991383"

    fun vk_api_id(): String {
        return client_id
    }

    fun vk_api_scope(): String {
        return api_scope
    }

    fun vk_api_display(): String {
        return api_display
    }

    fun vk_api_redirect_uri(): String {
        return api_redirect_uri
    }

    fun vk_api_response_type(): String {
        return api_response_type
    }

    fun version(): String {
        return version
    }


    fun api_secret_key(): String {
        return api_secret_key
    }

    fun max_loaded_in_screen(): Int {
        return max_loaded_in_screen
    }

    fun block_id(): String {
        return BLOCK_ID
    }
}
