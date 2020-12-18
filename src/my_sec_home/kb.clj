(ns my-sec-home.kb)
;
(def greeting-kb {:Type "keyboard"
                  :Buttons [{:Colums 3
                             :Rows 1
                             :Text "<font color=\"#494E67\">Easy start with My security home</font>"
                             :TextSize "large"
                             :TextHAlign "center"
                             :TextVAlign "middle"
                             :ActionType "reply"
                             :ActionBody "launch"
                             :BgColor "#f58d47"}]})
;
(def kb {:top-kb {:Type "keyboard"
                  :Buttons [{:Colums 3
                             :Rows 1
                             :Text "<font color=\"#494E67\">Settings @@@@@</font>"
                             :TextSize "large"
                             :TextHAlign "center"
                             :TextVAlign "middle"
                             :ActionType "reply"
                             :ActionBody "settings"
                             :BgColor "#f7bb3f"}
                            {:Colums 3
                             :Rows 1
                             :Text "<font color=\"#494E67\">@@@@@ takes photo</font>"
                             :TextSize "large"
                             :TextHAlign "center"
                             :TextVAlign "middle"
                             :ActionType "reply"
                             :ActionBody "take-photo"
                             :BgColor "#f6f7f9"}
                            {:Colums 3
                             :Rows 1
                             :Text "<font color=\"#494E67\">@@@@@ starts watchout</font>"
                             :TextSize "large"
                             :TextHAlign "center"
                             :TextVAlign "middle"
                             :ActionType "reply"
                             :ActionBody "start-watchout"
                             :BgColor "#7bf73f"}
                            ]}
         :stop-watchout-kb {:Type "keyboard"
                            :Buttons [{:Colums 3
                                       :Rows 1
                                       :Text "<font color=\"#494E67\">@@@@@ stops watchout</font>"
                                       :TextSize "large"
                                       :TextHAlign "center"
                                       :TextVAlign "middle"
                                       :ActionType "reply"
                                       :ActionBody "stop-watchout"
                                       :BgColor "#f75f3f"}]}
         :settings-kb {:Type "keyboard"
                        :Buttons [{:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E6'7>Let </font><b>@@@@@</b> loose"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "refuse"
                                   :BgColor "#dd64f5"}
                                  {:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E6'7>Change </font><b>@@@@@</b> </font><i>SENSIVITY</i> threshold"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "threshold sensivity"
                                   :BgColor "#96d2f2"}
                                  {:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E6'7>Change </font><b>@@@@@</b> </font><i>MAX AREA</i> threshold"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "threshold max_area"
                                   :BgColor "#c5deeb"}
                                  {:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E6'7>Get link to Google Photos album"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "get-link"
                                   :BgColor "#fcf917"}
                                  {:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E6'7>Erase </font><b>@@@@@'s</b> Google Photos album"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "wrap-erase-album"
                                   :BgColor "#17c3fc"}
                                  {:Colums 3
                                   :Rows 1
                                   :Text "<font color='#494E67'>Return to main menu"
                                   :TextSize "large"
                                   :TextHAlign "center"
                                   :TextVAlign "middle"
                                   :ActionType "reply"
                                   :ActionBody "escape"
                                   :BgColor "#bdf564"}]}})
(def change-button {:Colums 3
                    :Rows 1
                    :Text "<font color=\"#494E67\">Change </font><b>@@@@@</b>"
                    :TextSize "large"
                    :TextHAlign "center"
                    :TextVAlign "middle"
                    :ActionType "reply"
                    :ActionBody "choice-of-active active"
                    :BgColor "#b8fffb"})
;
(def choice-button {:Colums 3
                    :Rows 1
                    :Text "<font color=\"#494E67\">Change </font><b>@@@@@</b>"
                    :TextSize "large"
                    :TextHAlign "center"
                    :TextVAlign "middle"
                    :ActionType "reply"
                    :ActionBody "choice-of-active active"
                    :BgColor "#ebf2f2"})
;
;
(def refuse-button {:Colums 3
                    :Rows 1
                    :Text "<font color='#494E67>Let </font><b>@@@@@</b> loose"
                    :TextSize "large"
                    :TextHAlign "center"
                    :TextVAlign "middle"
                    :ActionType "reply"
                    :ActionBody "refuse"
                    :BgColor "#dd64f5"})
;
(def return-button {:Colums 3
                    :Rows 1
                    :Text "<font color='#494E67>Return to main menu"
                    :TextSize "large"
                    :TextHAlign "center"
                    :TextVAlign "middle"
                    :ActionType "reply"
                    :ActionBody "refuse"
                    :BgColor "#bdf564"})
;
#_(def settings-kb {:Type "keyboard"
                  :Buttons [refuse-button return-button]})
;
