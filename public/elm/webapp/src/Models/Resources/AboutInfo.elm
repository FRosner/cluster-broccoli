module Models.Resources.AboutInfo exposing (..)

import Models.Resources.UserInfo exposing (UserInfo)

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

type alias AboutInfo =
    { projectInfo : ProjectInfo
    , scalaInfo : ScalaInfo
    , sbtInfo : SbtInfo
    , authInfo : AuthInfo
    }
