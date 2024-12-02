package telran.employees;

import java.util.Map.*;
import java.util.concurrent.locks.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import telran.io.Persistable;

public class CompanyImpl implements Company, Persistable {
    private TreeMap<Long, Employee> employees = new TreeMap<>();
    private HashMap<String, List<Employee>> employeesDepartment = new HashMap<>();
    private TreeMap<Float, List<Manager>> managersFactor = new TreeMap<>();
    private final ReentrantReadWriteLock rwlock = new ReentrantReadWriteLock();
    private final Lock readLock = rwlock.readLock();
    private final Lock writeLock = rwlock.writeLock();

    private class EmployeeIterator implements Iterator<Employee> {
        private final Iterator<Employee> it = employees.values().iterator();
        private Employee prevEmployee;

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Employee next() {
            prevEmployee = it.next();
            return prevEmployee;
        }

        @Override
        public void remove() {
            it.remove();
            removeFromDepartment(prevEmployee);
            prevEmployee = null;
        }

    }

    @Override
    public Iterator<Employee> iterator() {
        return new EmployeeIterator();
    }

    @Override
    public void addEmployee(Employee empl) {
        writeLock.lock();
        try {
            if (employees.containsKey(empl.getId())) {
                throw new IllegalStateException();
            }
            employees.put(empl.getId(), empl);
            employeesDepartment.computeIfAbsent(empl.getDepartment(), k -> new ArrayList<>()).add(empl);
            if (empl instanceof Manager manager) {
                managersFactor.computeIfAbsent(manager.getFactor(), k -> new LinkedList<>()).add(manager);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Employee getEmployee(long id) {
        readLock.lock();
        try {
            return employees.get(id);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Employee removeEmployee(long id) {
        writeLock.lock();
        try {
            if (!employees.containsKey(id)) {
                throw new NoSuchElementException();
            }
            Employee removedEmployee = employees.remove(id);
            removeFromDepartment(removedEmployee);
            return removedEmployee;
        } finally {
            writeLock.unlock();
        }
    }

    private void removeFromDepartment(Employee removedEmployee) {
        String departmentOfRemovedEmployee = removedEmployee.getDepartment();
        List<Employee> departmentWithEmployees = employeesDepartment.get(departmentOfRemovedEmployee);
        departmentWithEmployees.remove(removedEmployee);
        if (departmentWithEmployees.isEmpty()) {
            employeesDepartment.remove(departmentOfRemovedEmployee);
        }
        if (removedEmployee instanceof Manager manager) {
            removeFactor(manager);
        }
    }

    private void removeFactor(Manager manager) {
        Float factor = manager.getFactor();
        List<Manager> managers = managersFactor.get(factor);
        if (managers != null) {
            managers.remove(manager);
        }
        managersFactor.remove(factor);
    }

    @Override
    public int getDepartmentBudget(String department) {
        readLock.lock();
        try {
            int budget = 0;
            List<Employee> employeesInDepartment = employeesDepartment.get(department);
            if (employeesInDepartment != null) {
                budget = employeesInDepartment.stream().mapToInt(Employee::computeSalary).sum();
            }
            return budget;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String[] getDepartments() {
        readLock.lock();
        try {
            return employeesDepartment.keySet().stream().sorted().toArray(String[]::new);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Manager[] getManagersWithMostFactor() {
        readLock.lock();
        try {
            Manager[] managers = new Manager[0];
            Entry<Float, List<Manager>> entry = managersFactor.lastEntry();
            if (entry != null) {
                managers = entry.getValue().stream().toArray(Manager[]::new);
            }
            return managers;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void saveToFile(String fileName) {
        readLock.lock();
        try {
            PrintWriter writer = new PrintWriter(fileName);
            forEach(writer::println);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void restoreFromFile(String fileName) {
        readLock.lock();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            reader.lines().forEach(line -> addEmployee(Employee.getEmployeeFromJSON(line)));
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException();
        }finally {
            readLock.unlock();
        }
    }

}
