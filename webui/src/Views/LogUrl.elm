module Views.LogUrl exposing (view)

import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.LogKind exposing (LogKind(..))


{-| Get the URL to a task log of an instance
-}
view : String -> AllocatedTask -> LogKind -> String
view jobId task kind =
    String.concat
        [ "/downloads/instances/"
        , jobId
        , "/allocations/"
        , task.allocationId
        , "/tasks/"
        , task.taskName
        , "/logs/"
        , case kind of
            StdOut ->
                "stdout"

            StdErr ->
                "stderr"

        -- Only fetch the last 500 KiB of the log, to avoid large requests and download times
        , "?offset=500KiB"
        ]
