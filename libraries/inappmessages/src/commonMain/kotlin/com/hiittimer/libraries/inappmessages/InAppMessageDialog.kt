package com.dangerfield.hiittimer.libraries.inappmessages

data class InAppMessageDialog(
    val title: String,
    val body: String,
    val image: InAppMessageDialogImage?,
    val positiveLabel: String,
    val negativeLabel: String,
)

enum class InAppMessageDialogImage {
    CreatorHeadshot,
}

enum class InAppMessageDialogResult {
    Positive,
    Negative,
    Dismissed,
}
