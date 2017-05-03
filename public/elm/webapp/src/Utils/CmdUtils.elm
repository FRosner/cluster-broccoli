module Utils.CmdUtils exposing (cmd, delay)

import Task

import Time exposing (Time)

import Utils.TaskUtils as TaskUtils

cmd : msg -> Cmd msg
cmd message =
  Task.perform identity (Task.succeed message)

delay : Time -> msg -> Cmd msg
delay time message =
  Task.succeed message
  |> TaskUtils.delay time
  |> Task.perform identity
