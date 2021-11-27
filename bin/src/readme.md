1-Object sensitivity


| Var          | Notation |
| :-           | :-       |
| Local var    | var@class.method#context |
| Static var   | var@class |
| Object field | heap.field |
| Static field | $\Delta(class.field)$ |
| Return var   | @ret@class.method#ctx |
| Parameter    | @param0@class.method#ctx |
| this         | @this@class.method#ctx |
| ArrayItem    | a.ArrayItem

| Statement | Action | Condition |
| :-        | :-     | :-        |
| a=new Object() @alloc | {alloc} $\to$ $\vec{a}$ | |
| a = x | $\vec x \to \vec a$ | $\vec x:edge$ |
| a = x.f | $\forall h \in \vec x, \vec{h.f} \to a$ | $\vec x:put, \vec{h.f}:edge $ |
| a.f = x | $\forall h \in \vec a, \vec x \to \vec {h.f} $ | $ \vec x, \vec a $ |
| Return x | $\vec x\to$ @ret@class.method#ctx | $\vec x:edge$ |
| a = o.f(args) #ctx | $\forall h \in \vec o, \{h\} \to @this...$ | $\vec o:call$|
| a = o.f(args) #ctx | $\forall h \in \vec o, arg1 \to @param1...$ | $\vec o:call$|
| a = o.f(args) #ctx | $\forall h \in \vec o, @ret... \to \vec a$ | $\vec o:call$|

Maintain `edge(x*, a)`, `put(x*, f, a)`, `get(a*, f, x)`, `call(o*, a, f, args, ctx)`

