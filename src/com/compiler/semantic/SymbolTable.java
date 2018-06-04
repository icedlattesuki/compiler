package com.compiler.semantic;

import com.compiler.semantic.type.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 符号表定义（支持嵌套作用域，采用红黑树存储）
 */
public class SymbolTable {
    private List<Map<String, Type>> stack = new ArrayList<>();

    public void create() {
        stack.add(new TreeMap<>());
    }

    public void remove() {
        stack.remove(stack.size() - 1);
    }

    public boolean add(String name, Type type) {
        Map<String, Type> currentMap = stack.get(stack.size() - 1);

        if (currentMap.get(name) == null) {
            currentMap.put(name, type);
            return true;
        } else {
            return false;
        }
    }

    public Type get(String name) {
        for (int i = stack.size() - 1; i >= 0;i--) {
            Map<String, Type> map = stack.get(i);

            if (map.get(name) != null) {
                return map.get(name);
            }
        }

        return null;
    }

    public Type getInCurrentScope(String name) {
        return stack.get(stack.size() - 1).get(name);
    }
}
