module Models.Ui.BodyUiModel exposing (..)

import Models.Resources.Template exposing (..)
import Models.Resources.Instance exposing (..)
import Models.Ui.InstanceParameterForm exposing (..)

import Set exposing (Set)
import Dict exposing (Dict)

type alias BodyUiModel =
  { expandedTemplates : Set TemplateId
  , selectedInstances : Set InstanceId
  , expandedInstances : Set InstanceId
  , instanceParameterForms : Dict InstanceId InstanceParameterForm
  , visibleEditInstanceSecrets : Set (InstanceId, String)
  , visibleNewTemplateSecrets : Set (TemplateId, String)
  , expandedNewInstanceForms : Dict TemplateId InstanceParameterForm
  }

initialModel =
  { expandedTemplates = Set.empty
  , selectedInstances = Set.empty
  , expandedInstances = Set.empty
  , instanceParameterForms = Dict.empty
  , visibleEditInstanceSecrets = Set.empty
  , visibleNewTemplateSecrets = Set.empty
  , expandedNewInstanceForms = Dict.empty
  }
