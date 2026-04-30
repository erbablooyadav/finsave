val timeFrameDays = 30
val result = System.currentTimeMillis() - (timeFrameDays * 24 * 60 * 60 * 1000L)
println("Current time: ${System.currentTimeMillis()}")
println("Calculated 30 days ago: $result")

val timeFrameDays90 = 90
val result90 = System.currentTimeMillis() - (timeFrameDays90 * 24 * 60 * 60 * 1000L)
println("Calculated 90 days ago: $result90")
