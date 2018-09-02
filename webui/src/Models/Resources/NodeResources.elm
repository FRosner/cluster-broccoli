module Models.Resources.NodeResources exposing (..)

import Array exposing (Array)
import Json.Decode as Decode exposing (field)


type alias AllocDirInfo =
    { available : Int
    , device : String
    , inodesUsedPercent : Float
    , mountPoint : String
    , size : Int
    , used : Int
    , usedPercent : Float
    }


type alias CPUInfo =
    { cpuName : String
    , idle : Float
    , system : Float
    , total : Float
    , user : Float
    }


type alias DiskInfo =
    AllocDirInfo


type alias MemoryInfo =
    { available : Int
    , free : Int
    , total : Int
    , used : Int
    }


type alias ResourceInfo =
    { allocDirStats : AllocDirInfo
    , cpusStats : Array CPUInfo
    , cpuTicksConsumed : Float
    , disksStats : Array DiskInfo
    , memoryStats : MemoryInfo
    , timestamp : Int
    , uptime : Int
    }


type alias NodeResources =
    { nodeId : String
    , nodeName : String
    , resources : ResourceInfo
    }


decoder =
    Decode.map3 NodeResources
        (field "nodeId" Decode.string)
        (field "nodeName" Decode.string)
        (field "resources" resourceInfoDecoder)


resourceInfoDecoder =
    Decode.map7 ResourceInfo
        (field "AllocDirStats" allocDirInfoDecoder)
        (field "CPU" (Decode.array cpuInfoDecoder))
        (field "CPUTicksConsumed" Decode.float)
        (field "DiskStats" (Decode.array diskInfoDecoder))
        (field "Memory" memoryInfoDecoder)
        (field "Timestamp" Decode.int)
        (field "Uptime" Decode.int)


allocDirInfoDecoder =
    Decode.map7 AllocDirInfo
        (field "Available" Decode.int)
        (field "Device" Decode.string)
        (field "InodesUsedPercent" Decode.float)
        (field "Mountpoint" Decode.string)
        (field "Size" Decode.int)
        (field "Used" Decode.int)
        (field "UsedPercent" Decode.float)


cpuInfoDecoder =
    Decode.map5 CPUInfo
        (field "CPU" Decode.string)
        (field "Idle" Decode.float)
        (field "System" Decode.float)
        (field "Total" Decode.float)
        (field "User" Decode.float)


diskInfoDecoder =
    allocDirInfoDecoder


memoryInfoDecoder =
    Decode.map4 MemoryInfo
        (field "Available" Decode.int)
        (field "Free" Decode.int)
        (field "Total" Decode.int)
        (field "Used" Decode.int)
