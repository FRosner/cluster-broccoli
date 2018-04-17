module Updates.Messages exposing (..)

import Http
import Dict exposing (Dict)
import Set exposing (Set)
import Models.Resources.UserInfo exposing (UserInfo)
import Models.Resources.Template exposing (TemplateId, Template, ParameterInfo)
import Models.Resources.Instance exposing (InstanceId, Instance)
import Models.Ui.InstanceParameterForm exposing (InstanceParameterForm)


type UpdateLoginStatusMsg
    = FetchLogin (Result Http.Error UserInfo)
    | FetchLogout (Result Http.Error String)
    | FetchVerify (Result Http.Error String)


type UpdateErrorsMsg
    = AddError String
    | CloseError Int


type UpdateLoginFormMsg
    = LoginAttempt String String
    | LogoutAttempt
    | EnterUserName String
    | EnterPassword String


type UpdateBodyViewMsg
    = ToggleTemplate TemplateId
    | InstanceSelected InstanceId Bool
    | AllInstancesSelected (Set InstanceId) Bool
    | InstanceExpanded InstanceId Bool
    | StartInstance InstanceId
    | StopInstance InstanceId
    | AllInstancesExpanded (Set InstanceId) Bool
    | EnterEditInstanceParameterValue Instance String String
    | SelectEditInstanceTemplate Instance (Dict TemplateId Template) TemplateId
    | EnterNewInstanceParameterValue TemplateId String String
    | ApplyParameterValueChanges Instance (Maybe InstanceParameterForm) Template
    | DiscardParameterValueChanges InstanceId
    | ToggleEditInstanceSecretVisibility InstanceId String
    | ToggleNewInstanceSecretVisibility TemplateId String
    | ExpandNewInstanceForm Bool TemplateId
    | SubmitNewInstanceCreation TemplateId (Dict String ParameterInfo) (Dict String (Maybe String))
    | DiscardNewInstanceCreation TemplateId
    | DeleteSelectedInstances TemplateId (Set String)
    | AttemptDeleteSelectedInstances TemplateId (Set String)
    | StartSelectedInstances (Set String)
    | StopSelectedInstances (Set String)
    | StopPeriodicJobs String (List String)
