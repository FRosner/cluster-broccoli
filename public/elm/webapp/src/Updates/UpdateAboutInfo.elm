module Updates.UpdateAboutInfo exposing (updateAboutInfo)

import Updates.Messages exposing (UpdateAboutInfoMsg(..), UpdateErrorsMsg(..))
import Models.Resources.AboutInfo exposing (AboutInfo)
import Messages exposing (AnyMsg(..))
import Utils.CmdUtils exposing (cmd)
import Http exposing (Error(..))

updateAboutInfo : UpdateAboutInfoMsg -> Maybe AboutInfo -> (Maybe AboutInfo, Cmd AnyMsg)
updateAboutInfo message oldAboutInfo =
  case message of
    FetchAbout (Ok newAboutInfo) ->
      ( Just newAboutInfo
      , Cmd.none
      )
    FetchAbout (Err error) ->
      ( Nothing
      , (cmd (UpdateErrorsMsg (AddError "Fetching 'about' failed.")))
      )

-- errorToString : Http.Error -> String
-- errorToString error =
--   case error of
--     BadStatus request ->
--       String.concat
--         [ "Fetching information about Cluster Broccoli failed: "
--         , ( toString request.status.code )
--         , " ("
--         , request.status.message
--         , ")"
--         ]
--     _ -> toString error
