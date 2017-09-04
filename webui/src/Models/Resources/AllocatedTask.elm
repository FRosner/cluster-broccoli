module Models.Resources.AllocatedTask exposing (..)

import Json.Decode as Decode exposing (Decoder, field, list, string, int, float, nullable)
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
    , resources : Resources
    }


{-| Resources of an allocated task
-}
type alias Resources =
    { cpuRequiredMhz : Maybe Float
    , cpuUsedMhz : Maybe Float
    , memoryRequiredBytes : Maybe Float
    , memoryUsedBytes : Maybe Float
    }


resourcesDecoder : Decoder Resources
resourcesDecoder =
    Decode.map4 Resources
        (field "cpuRequiredMhz" (nullable float))
        (field "cpuUsedMhz" (nullable float))
        (field "memoryRequiredBytes" (nullable float))
        (field "memoryUsedBytes" (nullable float))


{-| } Decode an allocated task from JSON.
-}
decoder : Decoder AllocatedTask
decoder =
    Decode.map5 AllocatedTask
        (field "taskName" string)
        (field "taskState" TaskState.decoder)
        (field "allocationId" string)
        (field "clientStatus" ClientStatus.decoder)
        (field "resources" resourcesDecoder)
