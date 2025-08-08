import React from 'react';

const Checkbox = React.forwardRef(({ className, ...props }, ref) => {
  return (
    <input
      type="checkbox"
      className={`h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 ${className}`}
      ref={ref}
      {...props}
    />
  );
});

Checkbox.displayName = "Checkbox";

export default Checkbox; 