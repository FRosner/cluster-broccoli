module Utils.CmdUtils exposing (cmd)

import Task

cmd : msg -> Cmd msg
cmd m =
  Task.perform identity (Task.succeed m)
