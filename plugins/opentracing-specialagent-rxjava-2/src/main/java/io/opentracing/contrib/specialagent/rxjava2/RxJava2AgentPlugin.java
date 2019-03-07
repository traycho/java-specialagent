/* Copyright 2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentracing.contrib.specialagent.rxjava2;

import static net.bytebuddy.matcher.ElementMatchers.*;

import java.util.Arrays;

import io.opentracing.contrib.specialagent.AgentPlugin;
import io.opentracing.contrib.specialagent.AgentPluginUtil;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Identified.Narrowable;
import net.bytebuddy.agent.builder.AgentBuilder.InitializationStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class RxJava2AgentPlugin implements AgentPlugin {
  @Override
  public Iterable<? extends AgentBuilder> buildAgent(final String agentArgs) {
    final Narrowable builder = new AgentBuilder.Default()
      .with(RedefinitionStrategy.RETRANSFORMATION)
      .with(InitializationStrategy.NoOp.INSTANCE)
      .with(TypeStrategy.Default.REDEFINE)
      .type(hasSuperType(named("io.reactivex.Observable")));

    return Arrays.asList(builder.transform(new Transformer() {
      @Override
      public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
        return builder.visit(Advice.to(OnEnter1.class, OnExit.class).on(named("subscribe").and(takesArguments(1))));
      }
    }), builder.transform(new Transformer() {
      @Override
      public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
        return builder.visit(Advice.to(OnEnter2.class, OnExit.class).on(named("subscribe").and(takesArguments(2))));
      }
    }), builder.transform(new Transformer() {
      @Override
      public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
        return builder.visit(Advice.to(OnEnter3.class, OnExit.class).on(named("subscribe").and(takesArguments(3))));
      }
    }), builder.transform(new Transformer() {
      @Override
      public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
        return builder.visit(Advice.to(OnEnter3.class, OnExit.class).on(named("subscribe").and(takesArguments(4))));
      }
    }));
  }

  public static class OnExit {
    @SuppressWarnings("unused")
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(@Advice.Return(readOnly = false, typing = Typing.DYNAMIC) Object returned, @Advice.Thrown(readOnly = false, typing = Typing.DYNAMIC) Throwable thrown, @Advice.Argument(value = 0, typing = Typing.DYNAMIC) Object onNext) {
      if (thrown instanceof NullPointerException && onNext == null) {
        thrown = null;
        returned = RxJava2AgentIntercept.disposable();
      }
    }
  }

  public static class OnEnter1 {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.This Object thiz, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Object onNext) {
      if (!AgentPluginUtil.isEnabled())
        return;

      final Object enter = RxJava2AgentIntercept.enter(thiz, 1, onNext, null, null, null);
      if (enter != RxJava2AgentIntercept.NULL)
        onNext = enter;
    }
  }

  public static class OnEnter2 {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.This Object thiz, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Object onNext, @Advice.Argument(value = 1, readOnly = false, typing = Typing.DYNAMIC) Object onError) {
      if (!AgentPluginUtil.isEnabled())
        return;

      final Object enter = RxJava2AgentIntercept.enter(thiz, 2, onNext, onError, null, null);
      if (enter != RxJava2AgentIntercept.NULL)
        onNext = enter;
    }
  }

  public static class OnEnter3 {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.This Object thiz, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Object onNext, @Advice.Argument(value = 1, readOnly = false, typing = Typing.DYNAMIC) Object onError, @Advice.Argument(value = 2, readOnly = false, typing = Typing.DYNAMIC) Object onComplete) {
      if (!AgentPluginUtil.isEnabled())
        return;

      final Object enter = RxJava2AgentIntercept.enter(thiz, 3, onNext, onError, onComplete, null);
      if (enter != RxJava2AgentIntercept.NULL)
        onNext = enter;
    }
  }

  public static class OnEnter4 {
    @Advice.OnMethodEnter
    public static void enter(final @Advice.This Object thiz, @Advice.Argument(value = 0, readOnly = false, typing = Typing.DYNAMIC) Object onNext, @Advice.Argument(value = 1, readOnly = false, typing = Typing.DYNAMIC) Object onError, @Advice.Argument(value = 2, readOnly = false, typing = Typing.DYNAMIC) Object onComplete, @Advice.Argument(value = 2, readOnly = false, typing = Typing.DYNAMIC) Object onSubscribe) {
      if (!AgentPluginUtil.isEnabled())
        return;

      final Object enter = RxJava2AgentIntercept.enter(thiz, 4, onNext, onError, onComplete, onSubscribe);
      if (enter != RxJava2AgentIntercept.NULL)
        onNext = enter;
    }
  }
}