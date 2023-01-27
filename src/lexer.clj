(ns lexer
  (:require [utils.lexing :refer [make-tokenizer]]))

(def ^:private tokens
  [{:regex #"\/\/.*"              :ignore true}
   {:regex #"^#"                  :keyword :hash}
   {:regex #"^if\b"               :keyword :if}
   {:regex #"^is\b"               :keyword :is}
   {:regex #"^not\b"              :keyword :not}
   {:regex #"^unless\b"           :keyword :unless}
   {:regex #"^else\b"             :keyword :else}
   {:regex #"^return\b"           :keyword :return}
   {:regex #"^let\b"              :keyword :let}
   {:regex #"^protocol\b"         :keyword :protocol}
   {:regex #"^for\b"              :keyword :for}
   {:regex #"^of\b"               :keyword :of}
   {:regex #"^impl\b"             :keyword :impl}
   {:regex #"^yield\b"            :keyword :yield}
   {:regex #"^async\b"            :keyword :async}
   {:regex #"^assert\!"           :keyword :assert!}
   {:regex #"^await\b"            :keyword :await}
   {:regex #"^new\b"              :keyword :new}
   {:regex #"^\&\&"               :keyword :and-and}
   {:regex #"^\|\|"               :keyword :or-or}
   {:regex #"^\=\=\="             :keyword :triple-eq}
   {:regex #"^\!\=\="             :keyword :triple-not-eq}
   {:regex #"^\=\="               :keyword :double-eq}
   {:regex #"^\!\="               :keyword :not-eq}
   {:regex #"^\!"                 :keyword :bang}
   {:regex #"^\="                 :keyword :eq}
   {:regex #"^fn\b"               :keyword :fn}
   {:regex #"^\{"                 :keyword :open-b}
   {:regex #"^\}"                 :keyword :close-b}
   {:regex #"^\("                 :keyword :open-p}
   {:regex #"^\)"                 :keyword :close-p}
   {:regex #"^[\-\+]?(\d*\.)?\d+" :keyword :num}
   {:regex #"^\.\.\."             :keyword :spread}
   {:regex #"^\."                 :keyword :dot}
   {:regex #"^\>\="               :keyword :gt-eq}
   {:regex #"^\<\="               :keyword :lt-eq}
   {:regex #"^\<\/"               :keyword :jsx-close}
   {:regex #"^\>"                 :keyword :gt}
   {:regex #"^\<"                 :keyword :lt}
   {:regex #"^\+"                 :keyword :plus}
   {:regex #"^\-"                 :keyword :minus}
   {:regex #"^\*\*"               :keyword :pow}
   {:regex #"^\*"                 :keyword :times}
   {:regex #"^\:\:"               :keyword :double-colon}
   {:regex #"^\:"                 :keyword :colon}
   {:regex #"^\/"                 :keyword :div}
   {:regex #"^\["                 :keyword :open-sq}
   {:regex #"^\]"                 :keyword :close-sq}
   {:regex #"(?s)\"([^\\\"]|\\.)*\"" :keyword :string-lit}
   {:regex #"^[a-zA-Z_\?\!\$]+"     :keyword :id}])

(def tokenize (make-tokenizer tokens))
