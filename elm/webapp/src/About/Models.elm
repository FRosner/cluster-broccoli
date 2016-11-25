module About.Models exposing (..)

import Maybe exposing (Maybe(..))

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

type alias UserInfo =
  { name : String
  , role : String
  , instanceRegex : String
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

-- TODO how to make the initial model? it needs to be empty because it is not yet retrieved
