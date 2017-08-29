module Models.Resources.AllocatedTask exposing (..)

import Json.Decode as Decode exposing (field, list, string, int, float, nullable)
import Models.Resources.TaskState as TaskState exposing (TaskState)
import Models.Resources.Allocation exposing (AllocationId)
import Models.Resources.ClientStatus as ClientStatus exposing (ClientStatus)


{-| A type for the name of a task.
-}
type alias TaskName =
    String


{-| An allocated task.
-}
type alias AllocatedTask =
    { taskName : TaskName
    , taskState : TaskState
    , allocationId : AllocationId
    , clientStatus : ClientStatus
    , cpuTicksUsed : Maybe Float
    , memoryBytesUsed : Maybe Int
    }


{-| Decode an allocated task from JSON.
-}
decoder : Decode.Decoder AllocatedTask
decoder =
    Decode.map6 AllocatedTask
        (field "taskName" string)
        (field "taskState" TaskState.decoder)
        (field "allocationId" string)
        (field "clientStatus" ClientStatus.decoder)
        (field "cpuTicksUsed" (nullable float))
        (field "memoryBytesUsed" (nullable int))
