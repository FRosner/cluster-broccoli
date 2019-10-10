module Views.LogUrl exposing (periodicTaskLog, taskLog)

import Models.Resources.AllocatedTask exposing (AllocatedTask)
import Models.Resources.LogKind exposing (LogKind(..))


{-| Get the URL to a task log of an instance
-}
taskLog : String -> AllocatedTask -> LogKind -> String
taskLog instanceId task kind =
    taskLogHelper instanceId Nothing task kind


{-| Get the URL to a periodic task log of an instance
-}
periodicTaskLog : String -> String -> AllocatedTask -> LogKind -> String
periodicTaskLog instanceId periodicJobId task kind =
    taskLogHelper instanceId (Just periodicJobId) task kind


taskLogHelper : String -> Maybe String -> AllocatedTask -> LogKind -> String
taskLogHelper instanceId maybePeriodicJobId task kind =
    String.concat
        [ "/downloads/instances/"
        , instanceId
        , maybePeriodicJobId
            |> Maybe.map (\i -> String.concat [ "/periodic/", i ])
            |> Maybe.withDefault ""
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
