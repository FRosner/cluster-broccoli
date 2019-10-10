module Models.Resources.InstanceTasks exposing (..)

import Dict exposing (Dict)
import Json.Decode as Decode exposing (Decoder)
import Models.Resources.AllocatedTask as AllocatedTask exposing (AllocatedTask)
import Models.Resources.Instance exposing (InstanceId)


{-| The tasks of an instance
-}
type alias InstanceTasks =
    { instanceId : InstanceId
    , allocatedTasks : List AllocatedTask
    , allocatedPeriodicTasks : Dict String (List AllocatedTask)
    }


empty : InstanceId -> InstanceTasks
empty instanceId =
    { instanceId = instanceId
    , allocatedTasks = []
    , allocatedPeriodicTasks = Dict.empty
    }


{-| Decode tasks of an instance from JSON.
-}
decoder : Decoder InstanceTasks
decoder =
    Decode.map3 InstanceTasks
        (Decode.field "instanceId" Decode.string)
        (Decode.field "allocatedTasks" (Decode.list AllocatedTask.decoder))
        (Decode.field "allocatedPeriodicTasks" (Decode.dict (Decode.list AllocatedTask.decoder)))
