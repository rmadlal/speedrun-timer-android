package il.ronmad.speedruntimer.web

sealed class Result<out T>
class Success<out T>(val value: T) : Result<T>()
class Failure<out T> : Result<T>()
