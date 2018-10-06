module Models.Resources.NodeResources exposing (..)

import Array exposing (Array)
import Dict exposing (Dict)
import Json.Decode as Decode exposing (field)


type alias TotalResources =
    { cpu : Int
    , memoryMB : Int
    , diskMB : Int
    }


type alias TotalUtilization =
    { cpu : Int
    , memory : Int
    }


type alias HostResources =
    { cpu : Int
    , memoryUsed : Int
    , memoryTotal : Int
    , diskUsed : Int
    , diskSize : Int
    }


type alias AllocatedResources =
    { id : String
    , name : String
    , cpu : Int
    , memoryMB : Int
    , diskMB : Int
    }


type alias AllocatedResourcesUtilization =
    { id : String
    , name : String
    , cpu : Int
    , memory : Int
    }

type alias NodeResources =
    { nodeId : String
    , nodeName : String
    , totalResources : TotalResources
    , hostResources : HostResources
    , allocatedResources : Dict String AllocatedResources
    , allocatedResourcesUtilization : Dict String AllocatedResourcesUtilization
    , totalAllocated : TotalResources
    , totalUtilized : TotalUtilization
    }


decoder =
    Decode.map8 NodeResources
        (field "nodeId" Decode.string)
        (field "nodeName" Decode.string)
        (field "totalResources" totalResourcesDecoder)
        (field "hostResources" hostResourcesDecoder)
        (field "allocatedResources" (Decode.dict allocatedResourcesDecoder))
        (field "allocatedResourcesUtilization" (Decode.dict allocatedResourcesUtilizationDecoder))
        (field "totalAllocated" totalResourcesDecoder)
        (field "totalUtilized" totalUtilizationDecoder)


totalResourcesDecoder =
    Decode.map3 TotalResources
        (field "cpu" Decode.int)
        (field "memoryMB" Decode.int)
        (field "diskMB" Decode.int)


totalUtilizationDecoder =
    Decode.map2 TotalUtilization
        (field "cpu" Decode.int)
        (field "memory" Decode.int)


hostResourcesDecoder =
    Decode.map5 HostResources
        (field "cpu" Decode.int)
        (field "memoryUsed" Decode.int)
        (field "memoryTotal" Decode.int)
        (field "diskUsed" Decode.int)
        (field "diskSize" Decode.int)


allocatedResourcesDecoder =
    Decode.map5 AllocatedResources
        (field "id" Decode.string)
        (field "name" Decode.string)
        (field "cpu" Decode.int)
        (field "memoryMB" Decode.int)
        (field "diskMB" Decode.int)


allocatedResourcesUtilizationDecoder =
    Decode.map4 AllocatedResourcesUtilization
        (field "id" Decode.string)
        (field "name" Decode.string)
        (field "cpu" Decode.int)
        (field "memory" Decode.int)
