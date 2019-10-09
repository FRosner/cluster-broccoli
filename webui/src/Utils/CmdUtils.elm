module Utils.CmdUtils exposing (delayMsg, sendMsg)

import Task
import Time exposing (Time)
import Utils.TaskUtils as TaskUtils


sendMsg : msg -> Cmd msg
sendMsg message =
    Task.perform identity (Task.succeed message)


delayMsg : Time -> msg -> Cmd msg
delayMsg time message =
    Task.succeed message
        |> TaskUtils.delay time
        |> Task.perform identity
