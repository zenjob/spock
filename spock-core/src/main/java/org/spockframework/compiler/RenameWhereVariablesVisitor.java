package org.spockframework.compiler;

import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.DynamicVariable;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.control.SourceUnit;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

public class RenameWhereVariablesVisitor extends ClassCodeVisitorSupport {

  private Map<String, String> renamingMap;

  public RenameWhereVariablesVisitor(Map<String, String> renamingMap) {
    this.renamingMap = renamingMap;
  }

  public void visitVariableExpression(VariableExpression expression) {
    if (renamingMap.containsKey(expression.getName())) {
      Field field = null;
      try {
        field = VariableExpression.class.getDeclaredField("variable");
        field.setAccessible(true);
        field.set(expression, renamingMap.get(expression.getName()));
      } catch (NoSuchFieldException | IllegalAccessException e) {
        return;
      }
      if (expression.getAccessedVariable() instanceof Parameter) {
        expression.setAccessedVariable(new Parameter(expression.getAccessedVariable().getType(), renamingMap.get(expression.getName())));
      } else {
        expression.setAccessedVariable(new DynamicVariable(renamingMap.get(expression.getName()), false));
      }
    }
  }

  public void visitClosureExpression(ClosureExpression expression) {
    renamingMap.values().forEach(key -> expression.getVariableScope().putReferencedClassVariable(new DynamicVariable(key, false)));
    super.visitClosureExpression(expression);
  }

  public void visitMethod(MethodNode node) {
    Parameter[] parameters = Arrays.stream(node.getParameters())
      .map(p -> {
        if (renamingMap.keySet().contains(p.getName())) {
          return new Parameter(p.getType(), renamingMap.get(p.getName()));
        } else {
          return p;
        }
      }).toArray(Parameter[]::new);
    node.setParameters(parameters);
    visitConstructorOrMethod(node, false);
  }

  @Override
  protected SourceUnit getSourceUnit() {
    return null;
  }
}
