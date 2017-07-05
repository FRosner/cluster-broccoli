module Utils.MaybeUtilsSuite exposing (tests)

import Utils.MaybeUtils as MaybeUtils
import Maybe exposing (..)
import Test exposing (..)
import Expect


tests : Test
tests =
    describe "MaybeUtils Tests"
        [ describe "concat Tests"
            [ test "Nothing" <|
                \() -> Expect.equal Nothing (MaybeUtils.concat Nothing)
            , test "Just Nothing" <|
                \() -> Expect.equal Nothing (MaybeUtils.concat (Just Nothing))
            , test "Just Something" <|
                \() -> Expect.equal (Just 5) (MaybeUtils.concat (Just (Just 5)))
            ]
        , describe "isDefined Tests"
            [ test "Nothing" <|
                \() -> Expect.false "Expected Nothing to be undefined" (MaybeUtils.isDefined Nothing)
            , test "Something" <|
                \() -> Expect.true "Expected Something to be defined" (MaybeUtils.isDefined (Just 5))
            ]
        ]
