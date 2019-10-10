module Models.Resources.AboutInfo exposing
    ( AboutInfo
    , asAuthInfoOf
    , asClusterManagerInfoOf
    , asServiceDiscoveryInfoOf
    , asServicesInfoOf
    , decoder
    )

import Json.Decode as Decode exposing (field)
import Models.Resources.UserInfo exposing (UserInfo, userInfoDecoder)


type alias ProjectInfo =
    { name : String
    , version : String
    }


type alias ScalaInfo =
    { version : String
    }


type alias SbtInfo =
    { version : String
    }


type alias AuthInfo =
    { enabled : Bool
    , userInfo : UserInfo
    }


type alias ServicesInfo =
    { clusterManagerInfo : ClusterManagerInfo
    , serviceDiscoveryInfo : ServiceDiscoveryInfo
    }


type alias ClusterManagerInfo =
    { connected : Bool
    }


type alias ServiceDiscoveryInfo =
    { connected : Bool
    }


type alias AboutInfo =
    { projectInfo : ProjectInfo
    , scalaInfo : ScalaInfo
    , sbtInfo : SbtInfo
    , authInfo : AuthInfo
    , services : ServicesInfo
    }


asServicesInfoOf : AboutInfo -> ServicesInfo -> AboutInfo
asServicesInfoOf aboutInfo servicesInfo =
    { aboutInfo | services = servicesInfo }


asAuthInfoOf : AboutInfo -> AuthInfo -> AboutInfo
asAuthInfoOf aboutInfo authInfo =
    { aboutInfo | authInfo = authInfo }


asServiceDiscoveryInfoOf : ServicesInfo -> ServiceDiscoveryInfo -> ServicesInfo
asServiceDiscoveryInfoOf servicesInfo serviceDiscoveryInfo =
    { servicesInfo | serviceDiscoveryInfo = serviceDiscoveryInfo }


asClusterManagerInfoOf : ServicesInfo -> ClusterManagerInfo -> ServicesInfo
asClusterManagerInfoOf servicesInfo clusterManagerInfo =
    { servicesInfo | clusterManagerInfo = clusterManagerInfo }


decoder =
    Decode.map5 AboutInfo
        (field "project" projectInfoDecoder)
        (field "scala" scalaInfoDecoder)
        (field "sbt" sbtInfoDecoder)
        (field "auth" authInfoDecoder)
        (field "services" servicesInfoDecoder)


projectInfoDecoder =
    Decode.map2 ProjectInfo
        (field "name" Decode.string)
        (field "version" Decode.string)


scalaInfoDecoder =
    Decode.map ScalaInfo
        (field "version" Decode.string)


sbtInfoDecoder =
    Decode.map SbtInfo
        (field "version" Decode.string)


authInfoDecoder =
    Decode.map2 AuthInfo
        (field "enabled" Decode.bool)
        (field "user" userInfoDecoder)


servicesInfoDecoder =
    Decode.map2 ServicesInfo
        (field "clusterManager" clusterManagerInfoDecoder)
        (field "serviceDiscovery" serviceDiscoveryInfoDecoder)


clusterManagerInfoDecoder =
    Decode.map ClusterManagerInfo
        (field "connected" Decode.bool)


serviceDiscoveryInfoDecoder =
    Decode.map ServiceDiscoveryInfo
        (field "connected" Decode.bool)
