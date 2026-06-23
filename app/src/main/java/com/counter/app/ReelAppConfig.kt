package com.counter.app

object ReelAppConfig {

    data class ReelAppData(
        val viewId: String,
        val requiresPresent: List<String> = emptyList(),
        val requiresAbsent: List<String> = emptyList(),
        val dynamicComparatorIds: List<String> = emptyList(),
        val cleanser: ((String) -> String)? = null,
        val eventType: Int = android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    )

    val reelData: Map<String, ReelAppData> = mapOf(

        "com.instagram.android" to ReelAppData(
            viewId = "id:com.instagram.android:id/clips_viewer_view_pager",
            requiresPresent = listOf("id:com.instagram.android:id/clips_ufi_component"),
            dynamicComparatorIds = listOf(
                "id:com.instagram.android:id/clips_captions_component",
                "id:com.instagram.android:id/clips_author_username"
            )
        ),

        "com.google.android.youtube" to ReelAppData(
            viewId = "id:com.google.android.youtube:id/reel_recycler",
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
            viewId = "desc:Tap to show video controls",
            dynamicComparatorIds = listOf(
                "path:android.widget.FrameLayout[0]>android.view.ViewGroup[0]>androidx.recyclerview.widget.RecyclerView[0]>android.view.ViewGroup[0]>android.view.ViewGroup[0]>android.widget.Button[0]>android.view.ViewGroup[2]",
                "path:android.widget.HorizontalScrollView[0]>androidx.viewpager.widget.ViewPager[0]>android.view.ViewGroup[0]>androidx.recyclerview.widget.RecyclerView[0]>android.view.ViewGroup[0]>android.view.ViewGroup[0]>android.widget.Button[0]>android.view.ViewGroup[2]>android.view.ViewGroup[0]>android.view.ViewGroup[0]"
            ),
            cleanser = { text ->
                text.replace("Story trayCreate storyCreate storyCreate storyClose import contactsFacebook is better with friendsFacebook is better with friendsSee stories from friends by adding people you know from your contacts.See stories from friends by adding people you know from your contacts.Find friends through contacts", "")
            }
        ),

        "com.facebook.orca" to ReelAppData(
            viewId = "desc:Tap to show video controls",
            dynamicComparatorIds = listOf(
                "path:android.widget.FrameLayout[0]>android.view.ViewGroup[0]>androidx.recyclerview.widget.RecyclerView[0]>android.view.ViewGroup[0]>android.view.ViewGroup[0]>android.widget.Button[0]>android.view.ViewGroup[2]"
            ),
            cleanser = { text ->
                text.replace("Story trayCreate storyCreate storyCreate storyClose import contactsFacebook is better with friendsFacebook is better with friends", "")
            }
        )
    )
}
