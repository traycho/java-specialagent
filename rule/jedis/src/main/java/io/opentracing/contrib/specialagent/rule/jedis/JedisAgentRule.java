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

package io.opentracing.contrib.specialagent.rule.jedis;

import static net.bytebuddy.matcher.ElementMatchers.*;

import io.opentracing.contrib.specialagent.AgentRule;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.AgentBuilder.Transformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.utility.JavaModule;

public class JedisAgentRule extends AgentRule {
  @Override
  public AgentBuilder buildAgentChainedGlobal1(final AgentBuilder builder) {
    return builder
      .type(hasSuperType(named("redis.clients.jedis.Connection")))
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(advice(typeDescription).to(SendCommand.class).on(named("sendCommand").and(takesArgument(1, byte[][].class))));
        }})
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(advice(typeDescription).to(ReadCommandOutput.class).on(named("readProtocolWithCheckingBroken")));
        }})
      .transform(new Transformer() {
        @Override
        public Builder<?> transform(final Builder<?> builder, final TypeDescription typeDescription, final ClassLoader classLoader, final JavaModule module) {
          return builder.visit(advice(typeDescription).to(OnError.class).on(named("readProtocolWithCheckingBroken")));
        }});
  }

  public static class SendCommand {
    @Advice.OnMethodEnter
    public static void enter(final @ClassName String className, final @Advice.Origin String origin, final @Advice.Argument(value = 0) Object command, final @Advice.Argument(value = 1, readOnly = false, typing = Typing.DYNAMIC) byte[][] args) {
      if (isAllowed(className, origin))
        JedisAgentIntercept.sendCommand(command, args);
    }
  }

  public static class ReadCommandOutput {
    @Advice.OnMethodExit
    public static void exit(final @ClassName String className, final @Advice.Origin String origin) {
      if (isAllowed(className, origin))
        JedisAgentIntercept.readCommandOutput();
    }
  }

  public static class OnError {
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(final @ClassName String className, final @Advice.Origin String origin, final @Advice.Thrown Throwable thrown) {
      if (isAllowed(className, origin))
        JedisAgentIntercept.onError(thrown);
    }
  }
}