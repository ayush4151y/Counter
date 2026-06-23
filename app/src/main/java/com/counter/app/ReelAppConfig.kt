package com.counter.app

object ReelAppConfig {

    data class ReelAppData(
        val viewIds: List<String>,
        val requiresPresent: List<String> = emptyList(),
        val requiresAbsent: List<String> = emptyList(),
        val dynamicComparatorIds: List<String> = emptyList(),
        val cleanser: ((String) -> String)? = null,
        val eventType: Int = android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SCROLLED
    )

    val reelData: Map<String, ReelAppData> = mapOf(

        "com.instagram.android" to ReelAppData(
            viewIds = listOf(
                "id:com.instagram.android:id/clips_viewer_view_pager",
                "id:com.instagram.android:id/reel_viewer_container",
                "desc:Double tap to like"
            ),
            requiresPresent = listOf("desc:Like"),
            dynamicComparatorIds = listOf(
                "id:com.instagram.android:id/clips_captions_component",
                "id:com.instagram.android:id/clips_author_username",
                "desc:Double tap to like"
            )
        ),

        "com.google.android.youtube" to ReelAppData(
            viewIds = listOf("id:com.google.android.youtube:id/reel_recycler"),
            dynamicComparatorIds = listOf("id:com.google.android.youtube:id/reel_player_page_content"),
            cleanser = { text ->
                if (text.contains("PostPostPostlike")) return@ReelAppData ""
                if (text.length <= 15) return@ReelAppData ""
                text.replace("Video Progress", "")
                    .replace("Tap to watch live", "")
                    .replace("Go to channel", "")
                    .replace("soundVideo ProgressSearchMoreHomeHomeShortsShortsCreateSubscriptions", "")
                    .replace("soundSearchMoreHomeHomeShortsShortsCreateSubscriptions", "")
            },
            eventType = android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ),

        "com.facebook.katana" to ReelAppData(
            viewIds = listOf(
                "desc:Tap to show video controls",
                "textContains:Reel"
            ),
            requiresPresent = listOf("textContains:Reel"),
            dynamicComparatorIds = listOf(
                "desc:Tap to show video controls",
                "textContains:Reel"
            ),
            cleanser = { text ->
                text.replace("Story trayCreate storyCreate storyCreate storyClose import contactsFacebook is better with friendsFacebook is better with friendsSee stories from friends by adding people you know from your contacts.See stories from friends by adding people you know from your contacts.Find friends through contacts", "")
                    .replace("Story", "")
                    .replace("Create story", "")
            }
        ),

        "com.facebook.orca" to ReelAppData(
            viewIds = listOf(
                "desc:Tap to show video controls",
                "textContains:Reel"
            ),
            requiresPresent = listOf("textContains:Reel"),
            dynamicComparatorIds = listOf(
                "desc:Tap to show video controls",
                "textContains:Reel"
            ),
            cleanser = { text ->
                text.replace("Story trayCreate storyCreate storyCreate storyClose import contactsFacebook is better with friendsFacebook is better with friends", "")
                    .replace("Story", "")
                    .replace("Create story", "")
            }
        )
    )
}
