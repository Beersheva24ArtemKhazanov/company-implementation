package telran.employees;

import org.json.JSONObject;

public class Employee {
    private long id;
    private int basicSalary;
    private String department;
    public Employee() {};
    @SuppressWarnings("unchecked")
    static public Employee getEmployeeFromJSON(String jsonStr) {
        JSONObject json = new JSONObject(jsonStr);
        String className = json.getString("className");
        try {
            Class<Employee> clazz = (Class<Employee>) Class.forName(className);
            Employee empl = clazz.getConstructor().newInstance();
            empl.setObject(json);
            return empl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setObject(JSONObject json) {
        id = json.getLong("id");
        basicSalary = json.getInt("basicSalary");
        department = json.getString("department");
    }

    public Employee(long id, int basicSalary, String department) {  
        this.id = id;
        this.basicSalary = basicSalary;
        this.department = department;
    }
    public int computeSalary() {
        return basicSalary;
    }

    public long getId() {
        return id;
    }

    public String getDepartment() {
        return department;
    }

    public int getBasicSalary() {
        return basicSalary;
    }

    @Override
    public boolean equals(Object obj) {
        Employee empl = (Employee)obj;
        return this.getId() == empl.getId();
    }

    @Override
    public String toString() {
        JSONObject json = new JSONObject();
        json.put("className", getClass().getName());
        fillJSON(json);
        return json.toString();
    }
    protected void fillJSON(JSONObject json) {
        json.put("id", id);
        json.put("basicSalary", basicSalary);
        json.put("department", department);
    }
}
