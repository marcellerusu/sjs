(ns parser
  (:require [utils.parsing :as p]))

(defn- remove-quotes [s]
  (subs s 1 (dec (count s))))

(declare parse-statement)
(declare parse-expr)
(declare parse-single-expr)

(defn- parse-str [tokens]
  (->> (p/from {:type :str} tokens)
       (p/one :string-lit :value)
       (p/fmap #(assoc % :value (-> % :value remove-quotes)))))

(defn- parse-id [tokens]
  (->> (p/from {:type :id-lookup} tokens)
       (p/one :id :name)))

(defn- parse-num [tokens]
  (->> (p/from {:type :num} tokens)
       (p/one :num :value)))

(defn- parse-dot [lhs tokens]
  (->> (p/from {:type :property-lookup, :lhs lhs} tokens)
       (p/skip :dot)
       (p/one :id :property)))

(defn- parse-fn-call [lhs tokens]
  (->> (p/from {:type :fn-call, :lhs lhs} tokens)
       (p/skip :open-p)
       (p/parse-until :close-p parse-expr :args)
       (p/skip :close-p)))

(def math-ops #{:plus :minus :lt :gt :lt-eq :gt-eq :times :pow :div})

(defn- parse-math-op [lhs tokens]
  (->> (p/from {:type :math-op, :lhs lhs} tokens)
       (p/either math-ops :op)
       (p/then parse-expr :rhs)))

(defn- parse-double-colon [lhs tokens]
  (->> (p/from {:type :bind, :lhs lhs} tokens)
       (p/skip :double-colon)
       (p/one :id :to)))

(defn- parse-double-eq [lhs tokens]
  (->> (p/from {:type :double-equals, :lhs lhs} tokens)
       (p/skip :double-eq)
       (p/then parse-expr :rhs)))

(defn- parse-not-eq [lhs tokens]
  (->> (p/from {:type :not-equals, :lhs lhs} tokens)
       (p/skip :not-eq)
       (p/then parse-expr :rhs)))

(defn- parse-dynamic-access [lhs tokens]
  (->> (p/from {:type :dynamic-access, :lhs lhs} tokens)
       (p/skip :open-sq)
       (p/then parse-expr :expr)
       (p/skip :close-sq)))

(defn- parse-triple-eq [lhs tokens]
  (->> (p/from {:type :triple-equals, :lhs lhs} tokens)
       (p/skip :triple-eq)
       (p/then parse-expr :rhs)))

(defn- parse-triple-not-eq [lhs tokens]
  (->> (p/from {:type :triple-not-equals, :lhs lhs} tokens)
       (p/skip :triple-not-eq)
       (p/then parse-expr :rhs)))

(defn- parse-is-not [lhs tokens]
  (->> (p/from {:type :is-not, :lhs lhs} tokens)
       (p/skip :is)
       (p/skip :not)
       (p/then parse-expr :rhs)))

(defn- parse-is [lhs tokens]
  (->> (p/from {:type :is, :lhs lhs} tokens)
       (p/skip :is)
       (p/then parse-expr :rhs)))

(defn- parse-is-or-is-not [lhs tokens]
  (->> (p/null tokens)
       (p/one-case
        {[:is :not] #(parse-is-not lhs %),
         :is #(parse-is lhs %)})))

(defn- parse-and-and [lhs tokens]
  (->> (p/from {:type :and-and, :lhs lhs} tokens)
       (p/skip :and-and)
       (p/then parse-expr :rhs)))

(defn- parse-or-or [lhs tokens]
  (->> (p/from {:type :or-or, :lhs lhs} tokens)
       (p/skip :or-or)
       (p/then parse-expr :rhs)))

(defn- parse-snd-assign [lhs tokens]
  (->> (p/from {:type :snd-assign, :lhs lhs} tokens)
       (p/skip :eq)
       (p/then parse-expr :rhs)))

(defn- parse-snd-expr [[lhs tokens]]
  (loop [lhs lhs, tokens tokens]
    (if-let [[expr tokens]
             (case (p/peek-next tokens)
               :dot (parse-dot lhs tokens)
               :open-p (parse-fn-call lhs tokens)
               :double-colon (parse-double-colon lhs tokens)
               :double-eq (parse-double-eq lhs tokens)
               :triple-eq (parse-triple-eq lhs tokens)
               :triple-not-eq (parse-triple-not-eq lhs tokens)
               :not-eq (parse-not-eq lhs tokens)
               :open-sq (parse-dynamic-access lhs tokens)
               :and-and (parse-and-and lhs tokens)
               :or-or (parse-or-or lhs tokens)
               :is (parse-is-or-is-not lhs tokens)
               :eq (parse-snd-assign lhs tokens)
               (when (math-ops (p/peek-next tokens)) (parse-math-op lhs tokens)))]
      (recur expr tokens)
      (p/from lhs tokens))))

(defn- parse-array [tokens]
  (->> (p/from {:type :array} tokens)
       (p/skip :open-sq)
       (p/parse-until :close-sq parse-expr :elements)
       (p/skip :close-sq)))

(defn- parse-assign-id [tokens]
  (->> (p/from {:type :id-assign} tokens)
       (p/one :id :name)))

(defn- parse-simple-name [tokens]
  (->> (p/null tokens)
       (p/one :id :name)
       (p/fmap :name)))

(defn- parse-assign-array [tokens]
  (->> (p/from {:type :array-deconstruction} tokens)
       (p/skip :open-sq)
       (p/parse-until :close-sq parse-simple-name :names)
       (p/skip :close-sq)))

(defn- parse-spread-assign [tokens]
  (->> (p/from {:type :spread-assign} tokens)
       (p/skip :spread)
       (p/one :id :name)))

(defn- parse-assign-expr [tokens]
  (->> (p/null tokens)
       (p/one-case
        {:id parse-assign-id,
         :open-sq parse-assign-array,
         :spread parse-spread-assign})))

(defn- parse-fn-name [tokens]
  (->> (p/null tokens)
       (p/one :id :name)
       (p/fmap :name)))

(defn- parse-args-def [tokens]
  (->> (p/from {:type :args} tokens)
       (p/skip :open-p)
       (p/parse-until :close-p parse-assign-expr)
       (p/skip :close-p)))

(defn- parse-fn-expr-body [tokens]
  (->> (p/from {:type :return} tokens)
       (p/skip :eq)
       (p/then parse-expr :expr)
       (p/fmap vector)))

(defn- parse-fn-body [tokens]
  (->> (p/null tokens)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement)
       (p/skip :close-b)))

(defn- parse-gen-modifier [tokens]
  (->> (p/null tokens)
       (p/skip :times)
       (p/fmap (fn [_] true))))

(defn- parse-async-modifier [tokens]
  (->> (p/null tokens)
       (p/skip :async)
       (p/fmap (fn [_] true))))

(defn- parse-fn [tokens]
  (->> (p/from {:type :fn} tokens)
       (p/one-case {:async parse-async-modifier} :async? false)
       (p/skip :fn)
       (p/one-case {:times parse-gen-modifier} :generator? false)
       (p/one-case {:id parse-fn-name} :name nil)
       (p/then parse-args-def :args)
       (p/one-case
        {:eq parse-fn-expr-body,
         :open-b parse-fn-body} :body)))

(defn- parse-set [tokens]
  (->> (p/from {:type :set} tokens)
       (p/skip :hash)
       (p/skip :open-b)
       (p/parse-until :close-b parse-expr :elements)
       (p/skip :close-b)))

(defn- parse-reg-obj-entry [tokens]
  (->> (p/from {:type :reg-obj-entry} tokens)
       (p/either #{:id :num} :key)
       (p/skip :colon)
       (p/then parse-expr :value)))

(defn- parse-obj-shorthand-entry [tokens]
  (->> (p/from {:type :obj-shorthand-entry} tokens)
       (p/one :id :id)))

(defn- parse-dynamic-obj-entry [tokens]
  (->> (p/from {:type :dynamic-obj-entry} tokens)
       (p/skip :open-sq)
       (p/then parse-expr :key-expr)
       (p/skip :close-sq)
       (p/skip :colon)
       (p/then parse-expr :value)))

(defn- parse-obj-entry [tokens]
  (->> (p/null tokens)
       (p/one-case
        {[:id :colon] parse-reg-obj-entry,
         [:num :colon] parse-reg-obj-entry,
         :id parse-obj-shorthand-entry,
         :open-sq parse-dynamic-obj-entry,
         :fn parse-fn})))

(defn- parse-obj [tokens]
  (->> (p/from {:type :obj-lit} tokens)
       (p/skip :open-b)
       (p/parse-until :close-b parse-obj-entry :entries)
       (p/skip :close-b)))

(defn- parse-bind-this [tokens]
  (->> (p/from {:type :bind-this} tokens)
       (p/skip :double-colon)
       (p/one :id :fn-name)))

(defn- parse-not [tokens]
  (->> (p/from {:type :not} tokens)
       (p/skip :bang)
       (p/then parse-expr :expr)))

(defn- parse-new [tokens]
  (->> (p/from {:type :new} tokens)
       (p/skip :new)
       (p/then parse-single-expr :expr)))

(defn- parse-spread [tokens]
  (->> (p/from {:type :spread} tokens)
       (p/skip :spread)
       (p/then parse-single-expr :expr)))

(defn- parse-yield [tokens]
  (->> (p/from {:type :yield} tokens)
       (p/skip :yield)
       (p/then parse-expr :expr)))

(defn- parse-await [tokens]
  (->> (p/from {:type :await} tokens)
       (p/skip :await)
       (p/then parse-expr :expr)))

(defn- parse-single-expr [tokens]
  (->> (p/null tokens)
       (p/one-case
        {:string-lit parse-str,
         :yield parse-yield,
         :await parse-await,
         :id parse-id,
         :num parse-num,
         :open-sq parse-array,
         :spread parse-spread,
         [:hash :open-b] parse-set,
         :open-b parse-obj,
         :double-colon parse-bind-this,
         :bang parse-not,
         [:async :fn] parse-fn,
         :fn parse-fn,
         :new parse-new})))

(defn- parse-expr [tokens]
  (->> (parse-single-expr tokens)
       parse-snd-expr))

(defn- parse-else-branch [tokens]
  (->> (p/null tokens)
       (p/skip :else)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement)
       (p/skip :close-b)))

(defn- parse-if [tokens]
  (->> (p/from {:type :if} tokens)
       (p/skip :if)
       (p/then parse-expr :expr)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement :pass)
       (p/skip :close-b)
       (p/one-case {:else parse-else-branch} :fail [])))

(defn- parse-unless [tokens]
  (->> (p/from {:type :unless} tokens)
       (p/skip :unless)
       (p/then parse-expr :expr)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement :body)
       (p/skip :close-b)))

(defn- parse-let [tokens]
  (->> (p/from {:type :let} tokens)
       (p/skip :let)
       (p/then parse-assign-expr :assign-expr)
       (p/skip :eq)
       (p/then parse-expr :rhs)))

(defn- parse-if-let [tokens]
  (->> (p/from {:type :if-let} tokens)
       (p/skip :if)
       (p/skip :let)
       (p/then parse-assign-expr :assign-expr)
       (p/skip :eq)
       (p/then parse-expr :expr)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement :pass)
       (p/skip :close-b)
       (p/one-case {:else parse-else-branch} :fail [])))

(defn- parse-impl [tokens]
  (->> (p/from {:type :impl-for} tokens)
       (p/skip :impl)
       (p/one :id :symbol-name)
       (p/skip :for)
       (p/one :id :constructor)
       (p/skip :eq)
       (p/then parse-expr :expr)))

(defn- parse-protocol [tokens]
  (->> (p/from {:type :protocol-def} tokens)
       (p/skip :protocol)
       (p/one :id :name)))

(defn- parse-return [tokens]
  (->> (p/from {:type :return} tokens)
       (p/skip :return)
       (p/then parse-expr :expr)))

(defn- parse-for-loop [tokens]
  (->> (p/from {:type :for-loop} tokens)
       (p/skip :for)
       (p/skip :let)
       (p/then parse-assign-expr :assign-expr)
       (p/skip :of)
       (p/then parse-expr :iterable-expr)
       (p/skip :open-b)
       (p/parse-until :close-b parse-statement :body)
       (p/skip :close-b)))

(defn- parse-id-assign [tokens]
  (->> (p/from {:type :id-assign} tokens)
       (p/one :id :name)
       (p/skip :eq)
       (p/then parse-expr :expr)))

(defn- parse-statement [tokens]
  (->> (p/null tokens)
       (p/one-case
        (array-map
         [:if :let] parse-if-let,
         :if parse-if,
         [:id :eq] parse-id-assign,
         :unless parse-unless,
         :impl parse-impl,
         :protocol parse-protocol,
         :let parse-let,
         :return parse-return,
         :for parse-for-loop,
         'otherwise parse-expr))))

(defn parse [tokens]
  (loop [ast-list [], rest-tokens tokens]
    (if (empty? rest-tokens)
      ast-list
      (let [[ast-node rest-tokens] (parse-statement rest-tokens)]
        (recur (conj ast-list ast-node) rest-tokens)))))
