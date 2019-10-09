module Utils.TaskUtils exposing (delay)

import Process
import Task exposing (Task)
import Time exposing (Time)


{-| Delay a task a given amount of `Time`
-}
delay : Time -> Task error value -> Task error value
delay time task =
    Process.sleep time
        |> Task.andThen (\_ -> task)
