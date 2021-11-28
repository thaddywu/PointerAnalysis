# 1-Context & Field sensitive Analysis

## 大体思想

实现最简单的 Anderson 算法，不考虑流敏感。讲变量分为静态域、局部变量，他们都可以指向堆上的对象，而堆上的对象可以持有很多域，这些域也被看成变量，并可以指向其他堆上的对象，此处便实现了域敏感；而对于每个方法，复制若干份，用调用点的编号作为上下文。

实现上，需要注意函数的拷贝应该是懒惰的，只有需要该拷贝的时候才会建立对应的控制流图。

## 变量的设计

变量有多种类型，设计成多种命名方式。
| Var          | Notation |
| :-           | :-       |
| Local var    | var@class.method#context |
| Static var   | var@class |
| Object field | heap.field |
| Static field | class.field |
| Return var   | @ret@class.method#ctx |
| Parameter    | @param0@class.method#ctx |
| this         | @this@class.method#ctx |
| ArrayItem    | a.ArrayItem |

要注意静态域的类，必须写成继承过来的类；将数字的任何访问都指向一个域 `ArrayItem`；对于 java 自带的 HashMap，List 也和数组一样处理；非静态域的类，认为是一个堆上对象持有的变量。

## 更新
非静态方法的调用需要注意填 this 域。

| Statement | Action | Condition |
| :-        | :-     | :-        |
| a=new Object() @alloc | {alloc} $\to$ $\vec{a}$ | |
| a = x | $\vec x \to \vec a$ | $\vec x:edge$ |
| a = x.f | $\forall h \in \vec x, \vec{h.f} \to a$ | $\vec x:put, \vec{h.f}:edge $ |
| a.f = x | $\forall h \in \vec a, \vec x \to \vec {h.f} $ | $ \vec x:edge, \vec a:get $ |
| Return x | $\vec x\to$ @ret@class.method#ctx | $\vec x:edge$ |
| a = o.f(args) #ctx | $\forall h \in \vec o, \{h\} \to @this...$ | $\vec o:call$|
| a = o.f(args) #ctx | $\forall h \in \vec o, arg1 \to @param1...$ | $\vec o:call$|
| a = o.f(args) #ctx | $\forall h \in \vec o, @ret... \to \vec a$ | $\vec o:call$|

通过 `edge(x*, a)`, `put(x*, f, a)`, `get(a*, f, x)`, `call(o*, a, f, args, ctx)` 就可以高效实现不动点算法。

## 分工
1800012913 吴耀轩 设计算法具体细节，写分析代码
1800013108 孙昊楠 写自动化测试，写样例，讨论多态调用和 Soot 接口的问题