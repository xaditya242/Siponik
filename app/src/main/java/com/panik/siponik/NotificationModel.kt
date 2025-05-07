data class NotificationModel(
    val title: String = "",
    val message: String = "",
    var isRead: Boolean = false,
    val timestamp: Long = 0L
)
